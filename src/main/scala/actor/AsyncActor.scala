package ornicar.scalalib
package actor

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.collection.immutable.Queue
import scala.concurrent.{ ExecutionContext, Future, Promise }

/*
 * Sequential like an actor, but for async functions,
 * and using an atomic backend instead of akka actor.
 */
abstract class AsyncActor(monitor: AsyncActor.Monitor)(using ExecutionContext):

  import AsyncActor.*

  // implement async behaviour here
  protected val process: ReceiveAsync

  def !(msg: Matchable): Unit =
    if stateRef.getAndUpdate(state => Some(state.fold(Queue.empty[Matchable])(_.enqueue(msg)))).isEmpty then
      run(msg)

  def ask[A](makeMsg: Promise[A] => Matchable): Future[A] =
    val promise = Promise[A]()
    this ! makeMsg(promise)
    promise.future

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private val stateRef: AtomicReference[State] = new AtomicReference(None)

  private def run(msg: Matchable): Unit =
    process.applyOrElse(msg, fallback).onComplete(postRun)

  private val postRun = (_: Matchable) =>
    stateRef.getAndUpdate(postRunUpdate).flatMap(_.headOption).foreach(run)

  private val fallback = (msg: Matchable) =>
    monitor.unhandled(msg)
    Future.unit

object AsyncActor:

  type ReceiveAsync = PartialFunction[Matchable, Future[Matchable]]

  case class Monitor(unhandled: Any => Unit)
  // lila.log("asyncActor").warn(s"unhandled msg: $msg")

  private type State = Option[Queue[Matchable]]

  private val postRunUpdate = new UnaryOperator[State]:
    override def apply(state: State): State =
      state.flatMap: q =>
        if q.isEmpty then None else Some(q.tail)

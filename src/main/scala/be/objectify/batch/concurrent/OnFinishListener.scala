package be.objectify.batch.concurrent

/**
 *
 */
trait OnFinishListener {
  def jobFinished(processed: Int, errors: Int)
}

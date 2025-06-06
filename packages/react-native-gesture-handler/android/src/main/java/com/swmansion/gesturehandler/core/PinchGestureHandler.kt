package com.swmansion.gesturehandler.core

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.swmansion.gesturehandler.react.eventbuilders.PinchGestureHandlerEventDataBuilder
import kotlin.math.abs

class PinchGestureHandler : GestureHandler() {
  var scale = 0.0
    private set
  var velocity = 0.0
    private set
  var focalPointX: Float = Float.NaN
    private set
  var focalPointY: Float = Float.NaN
    private set

  private var scaleGestureDetector: ScaleGestureDetector? = null
  private var startingSpan = 0f
  private var spanSlop = 0f
  private val gestureListener: ScaleGestureDetector.OnScaleGestureListener = object :
    ScaleGestureDetector.OnScaleGestureListener {
    override fun onScale(detector: ScaleGestureDetector): Boolean {
      val prevScaleFactor: Double = scale
      scale *= detector.scaleFactor.toDouble()
      val delta = detector.timeDeltaSeconds

      if (delta > 0) {
        velocity = (scale - prevScaleFactor) / delta
      }
      if (abs(startingSpan - detector.currentSpan) >= spanSlop &&
        state == STATE_BEGAN
      ) {
        activate()
      }
      return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
      startingSpan = detector.currentSpan
      return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
      // ScaleGestureDetector thinks that when fingers are 27mm away that's a sufficiently good
      // reason to trigger this method giving us no other choice but to ignore it completely.
    }
  }

  override fun onHandle(event: MotionEvent, sourceEvent: MotionEvent) {
    if (state == STATE_UNDETERMINED) {
      val context = view!!.context
      resetProgress()
      scaleGestureDetector = ScaleGestureDetector(context, gestureListener)
      val configuration = ViewConfiguration.get(context)
      spanSlop = configuration.scaledTouchSlop.toFloat()

      // set the focal point to the position of the first pointer as NaN causes the event not to arrive
      this.focalPointX = event.x
      this.focalPointY = event.y

      begin()
    }
    scaleGestureDetector?.onTouchEvent(sourceEvent)
    scaleGestureDetector?.let {
      val point = transformPoint(PointF(it.focusX, it.focusY))
      this.focalPointX = point.x
      this.focalPointY = point.y
    }

    if (sourceEvent.actionMasked == MotionEvent.ACTION_UP) {
      if (state == STATE_ACTIVE) {
        end()
      } else {
        fail()
      }
    }
  }

  override fun activate(force: Boolean) {
    // reset scale if the handler has not yet activated
    if (state != STATE_ACTIVE) {
      resetProgress()
    }
    super.activate(force)
  }

  override fun onReset() {
    scaleGestureDetector = null
    focalPointX = Float.NaN
    focalPointY = Float.NaN
    resetProgress()
  }

  override fun resetProgress() {
    velocity = 0.0
    scale = 1.0
  }

  class Factory : GestureHandler.Factory<PinchGestureHandler>() {
    override val type = PinchGestureHandler::class.java
    override val name = "PinchGestureHandler"

    override fun create(context: Context?): PinchGestureHandler = PinchGestureHandler()

    override fun createEventBuilder(handler: PinchGestureHandler) = PinchGestureHandlerEventDataBuilder(handler)
  }
}

package com.swmansion.gesturehandler.react

import android.util.SparseArray
import com.facebook.react.bridge.ReadableMap
import com.swmansion.gesturehandler.core.GestureHandler
import com.swmansion.gesturehandler.core.GestureHandlerInteractionController
import com.swmansion.gesturehandler.core.NativeViewGestureHandler

class RNGestureHandlerInteractionManager : GestureHandlerInteractionController {
  private val waitForRelations = SparseArray<IntArray>()
  private val simultaneousRelations = SparseArray<IntArray>()
  private val blockingRelations = SparseArray<IntArray>()

  fun dropRelationsForHandlerWithTag(handlerTag: Int) {
    waitForRelations.remove(handlerTag)
    simultaneousRelations.remove(handlerTag)
  }

  private fun convertHandlerTagsArray(config: ReadableMap, key: String): IntArray {
    val array = config.getArray(key)!!
    return IntArray(array.size()).also {
      for (i in it.indices) {
        it[i] = array.getInt(i)
      }
    }
  }

  fun configureInteractions(handler: GestureHandler, config: ReadableMap) {
    handler.setInteractionController(this)
    if (config.hasKey(KEY_WAIT_FOR)) {
      val tags = convertHandlerTagsArray(config, KEY_WAIT_FOR)
      waitForRelations.put(handler.tag, tags)
    }
    if (config.hasKey(KEY_SIMULTANEOUS_HANDLERS)) {
      val tags = convertHandlerTagsArray(config, KEY_SIMULTANEOUS_HANDLERS)
      simultaneousRelations.put(handler.tag, tags)
    }
    if (config.hasKey(KEY_BLOCKS_HANDLERS)) {
      val tags = convertHandlerTagsArray(config, KEY_BLOCKS_HANDLERS)
      blockingRelations.put(handler.tag, tags)
    }
  }

  override fun shouldWaitForHandlerFailure(handler: GestureHandler, otherHandler: GestureHandler) =
    waitForRelations[handler.tag]?.any { tag -> tag == otherHandler.tag } ?: false

  override fun shouldRequireHandlerToWaitForFailure(handler: GestureHandler, otherHandler: GestureHandler) =
    blockingRelations[handler.tag]?.any { tag -> tag == otherHandler.tag } ?: false

  override fun shouldHandlerBeCancelledBy(handler: GestureHandler, otherHandler: GestureHandler): Boolean {
    if (otherHandler is NativeViewGestureHandler) {
      return otherHandler.disallowInterruption
    }

    if (otherHandler is RNGestureHandlerRootHelper.RootViewGestureHandler) {
      return true
    }

    return false
  }
  override fun shouldRecognizeSimultaneously(handler: GestureHandler, otherHandler: GestureHandler) =
    simultaneousRelations[handler.tag]?.any { tag -> tag == otherHandler.tag } ?: false

  fun reset() {
    waitForRelations.clear()
    simultaneousRelations.clear()
  }

  companion object {
    private const val KEY_WAIT_FOR = "waitFor"
    private const val KEY_SIMULTANEOUS_HANDLERS = "simultaneousHandlers"
    private const val KEY_BLOCKS_HANDLERS = "blocksHandlers"
  }
}

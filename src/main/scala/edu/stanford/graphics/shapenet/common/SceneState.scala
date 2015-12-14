package edu.stanford.graphics.shapenet.common

/**
  * State of a scene (scene + selections)
  * @author Angel Chang
  */
case class SceneState(scene: Scene,
                      var selections: Seq[SceneSelection] = Seq()) {
}
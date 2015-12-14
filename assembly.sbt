test in assembly := {}

mainClass in assembly := Some("edu.stanford.graphics.shapenet.jme3.viewer.Viewer")

// This is a nasty hack.
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case x => {
      val oldstrat = old(x)
      if (oldstrat == MergeStrategy.deduplicate) MergeStrategy.first
      else oldstrat
    }
  }
}
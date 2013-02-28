package org.dbpedia.lookup

import org.dbpedia.lookup.lucene._
import org.dbpedia.lookup.server._
import java.io.File

object TestUtils {

  def tempDirectory : File = {
    val file = File.createTempFile("lookup", "")
    file.delete
    file.mkdir
    file
  }

  def tempPort : Int = new java.net.ServerSocket(0).getLocalPort

}


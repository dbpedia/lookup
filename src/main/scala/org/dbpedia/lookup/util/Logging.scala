package org.dbpedia.lookup.util

import org.slf4j.LoggerFactory

trait Logging {
  protected val logger  = LoggerFactory.getLogger(getClass.getName)
}

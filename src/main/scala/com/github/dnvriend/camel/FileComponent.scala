/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend.camel

/**
 * The Camel File Component is both a `Batch Consumer` and a `Polling Consumer`
 * Batch Consumer is basically a Polling Consumer that is capable of polling multiple Exchanges in a pool.
 */
object FileComponent extends CamelComponent {

  override def url: String = "file://"

  /**
   * Headers that can be used for the File Component
   */
  object Header {
    /**
     * The headers that are available for the Consumer only
     */
    object Consumer {
      /**
       * Only the file name (the name with no leading paths).
       */
      final val CamelFileNameOnly = "CamelFileNameOnly"

      /**
       * Name of the consumed file as a relative file path with offset from the starting directory configured on the endpoint.
       */
      final val CamelFileName = "CamelFileName"

      /**
       * The file path. For relative files this is the starting directory + the relative filename. For absolute files this is the absolute path.
       */
      final val CamelFilePath = "CamelFilePath"
      /**
       * The absolute path to the file. For relative files this path holds the relative path instead.
       */
      final val CamelFileAbsolutePath = "CamelFileAbsolutePath"

      /**
       * A long value containing the file size.
       */
      final val CamelFileLength = "CamelFileLength"

      /**
       * A Long value containing the last modified timestamp of the file.
       */
      final val CamelFileLastModified = "CamelFileLastModified"
    }
  }

  /**
   * Uri options
   */
  object Options {
    /**
     * An integer to define a maximum messages to gather per poll.
     * Set a value of 0 or negative to disabled it.
     */
    final val MaxMessagesPerPoll = "maxMessagesPerPoll"

    /**
     * Ant style filter exclusion.
     */
    final val AntInclude = "antInclude"

    /**
     * Milliseconds before polling the file/directory starts.
     */
    final val InitialDelay = "initialDelay"

    /**
     * Milliseconds before the next poll of the file/directory.
     */
    final val Delay = "delay"

    /**
     * If true, the file will be deleted after it is processed successfully.
     */
    final val Delete = "delete"
  }
}

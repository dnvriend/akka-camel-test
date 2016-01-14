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

object QueryStringBuilder {
  def build(queryParams: Map[String, String]): String =
    if (queryParams.nonEmpty)
      "?" + queryParams
        .filterNot { case (key, value) ⇒ key.length == 0 }
        .toList
        .sortBy { case (key, value) ⇒ key }
        .map { case (key, value) ⇒ s"$key=$value" }
        .mkString("&")
    else ""

  def apply(component: CamelComponent, directoryName: String, queryParams: Map[String, String]): String =
    component.url + directoryName + build(queryParams)
}

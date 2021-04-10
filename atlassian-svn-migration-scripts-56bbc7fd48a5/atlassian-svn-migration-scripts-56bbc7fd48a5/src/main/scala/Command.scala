/**
 * Copyright 2012 Atlassian
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.atlassian.svn2git

trait Command {
  val name: String
  val usage: Option[String] = None
  val help: String
  val available: Boolean = true

  def parse(arguments: Array[String]): Either[String, (Array[String], Array[String])]
  def apply(cmd: Cmd, options: Array[String], arguments: Array[String]): Boolean
}

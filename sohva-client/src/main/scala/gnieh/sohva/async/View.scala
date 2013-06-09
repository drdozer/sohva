/*
* This file is part of the sohva project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package gnieh.sohva
package async

import dispatch._
import Defaults._

import net.liftweb.json._

/** A view can be queried to get the result.
 *
 *  @author Lucas Satabin
 */
case class View[Key: Manifest, Value: Manifest, Doc: Manifest](design: Design,
                                                               view: String) extends gnieh.sohva.View[Key, Value, Doc] {

  type Result[T] = Future[Either[(Int, Option[ErrorResult]), T]]

  import design.db.couch.serializer
  import serializer.formats

  private def request = design.request / "_view" / view

  def query(key: Option[Key] = None,
            keys: List[Key] = Nil,
            startkey: Option[Key] = None,
            startkey_docid: Option[String] = None,
            endkey: Option[Key] = None,
            endkey_docid: Option[String] = None,
            limit: Int = -1,
            stale: Option[String] = None,
            descending: Boolean = false,
            skip: Int = 0,
            group: Boolean = false,
            group_level: Int = -1,
            reduce: Boolean = true,
            include_docs: Boolean = false,
            inclusive_end: Boolean = true,
            update_seq: Boolean = false): Result[ViewResult[Key, Value, Doc]] = {

    // build options
    val options = List(
      key.map(k => "key" -> serializer.toJson(k)),
      if (keys.nonEmpty) Some("keys" -> serializer.toJson(keys)) else None,
      startkey.map(k => "startkey" -> serializer.toJson(k)),
      startkey_docid.map("startkey_docid" -> _),
      endkey.map(k => "endkey" -> serializer.toJson(k)),
      endkey_docid.map("endkey_docid" -> _),
      if (limit > 0) Some("limit" -> limit) else None,
      stale.map("stale" -> _),
      if (descending) Some("descending" -> true) else None,
      if (skip > 0) Some("skip" -> skip) else None,
      if (group) Some("group" -> true) else None,
      if (group_level >= 0) Some("group_level" -> group_level) else None,
      if (reduce) None else Some("reduce" -> false),
      if (include_docs) Some("include_docs" -> true) else None,
      if (inclusive_end) None else Some("inclusive_end" -> false),
      if (update_seq) Some("update_seq" -> true) else None)
      .flatten
      .filter(_ != null) // just in case somebody gave Some(null)...
      .map {
        case (name, value) => (name, value.toString)
      }

    for(res <- design.db.couch.http(request <<? options).right)
      yield viewResult[Key,Value,Doc](res)

  }

  // helper methods

  private def viewResult[Key: Manifest, Value: Manifest, Doc: Manifest](json: String) = {
    val ast = parse(json)
    val res = for {
      total_rows <- (ast \ "total_rows").extractOpt[Int]
      offset <- (ast \ "offset").extractOpt[Int]
      JArray(rows) = (ast \ "rows")
    } yield ViewResult(total_rows, offset, rows.flatMap { row =>
        for {
          id <- (row \ "id").extractOpt[String]
          key <- (row \ "key").extractOpt[Key]
          value <- (row \ "value").extractOpt[Value]
          doc = (row \ "doc").extractOpt[Doc]
        } yield Row(id, key, value, doc)
    })
    res.getOrElse(ViewResult(0, 0, Nil))
  }

}

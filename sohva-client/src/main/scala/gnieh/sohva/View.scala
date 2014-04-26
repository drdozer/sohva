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

/** A view can be queried to get the result.
 *
 *  @author Lucas Satabin
 */
trait View[Result[_], Key, Value, Doc] {

  /** Queries the view on the server and returned the typed result.
   *  BE CAREFUL: If the types given to the constructor are not correct,
   *  strange things may happen! By 'strange', I mean exceptions
   */
  def query(
    key: Option[Key] = None,
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
    update_seq: Boolean = false): Result[ViewResult[Key, Value, Doc]]

}

case class ViewDoc(map: String, reduce: Option[String])

final case class ViewResult[Key, Value, Doc](
    total_rows: Int,
    offset: Int,
    update_seq: Option[Int],
    rows: List[Row[Key, Value, Doc]]) {

  self =>

  def values: List[(Key, Value)] =
    for (row <- rows)
      yield (row.key, row.value)

  def docs: List[(Key, Doc)] =
    for {
      row <- rows
      doc <- row.doc
    } yield (row.key, doc)

  def foreach(f: Row[Key, Value, Doc] => Unit): Unit =
    rows.foreach(f)

  def map[Key1, Value1, Doc1](f: Row[Key, Value, Doc] => Row[Key1, Value1, Doc1]): ViewResult[Key1, Value1, Doc1] =
    ViewResult(total_rows, offset, update_seq, rows.map(f))

  def flatMap[Key1, Value1, Doc1](f: Row[Key, Value, Doc] => ViewResult[Key1, Value1, Doc1]): ViewResult[Key1, Value1, Doc1] = {
    val results = rows.map(f)
    ViewResult(results.map(_.total_rows).sum, offset, update_seq, results.flatMap(_.rows))
  }

  def withFilter(p: Row[Key, Value, Doc] => Boolean): WithFilter =
    new WithFilter(p)

  class WithFilter(p: Row[Key, Value, Doc] => Boolean) {

    def foreach(f: Row[Key, Value, Doc] => Unit): Unit =
      for {
        row <- rows
        if p(row)
      } f(row)

    def map[Key1, Value1, Doc1](f: Row[Key, Value, Doc] => Row[Key1, Value1, Doc1]): ViewResult[Key1, Value1, Doc1] =
      ViewResult(rows.size, offset, update_seq, for {
        row <- rows
        if p(row)
      } yield f(row))

    def flatMap[Key1, Value1, Doc1](f: Row[Key, Value, Doc] => ViewResult[Key1, Value1, Doc1]): ViewResult[Key1, Value1, Doc1] = {
      val rows1 = for {
        row <- rows
        if p(row)
        row1 <- f(row).rows
      } yield row1
      ViewResult(rows1.size, offset, update_seq, rows1)
    }

    def withFilter(q: Row[Key, Value, Doc] => Boolean): WithFilter =
      new WithFilter(row => p(row) && q(row))

  }

}

case class Row[Key, Value, Doc](
  id: Option[String],
  key: Key,
  value: Value,
  doc: Option[Doc] = None)


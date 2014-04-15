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
package lucene

import akka.actor.ActorSystem
import akka.util.Timeout

import scala.collection.{ mutable => mu }

import scala.concurrent.Future

import org.apache.lucene.index.IndexWriter

import async._

/** A CouchDB Indexer gives access to indexing of databases that are inside a CouchDB instance.
 *  Several databases of the CouchDB server may be indexed either separatly or in the same index.
 *  Different credentials may be used for different indexed databases.
 *
 *  @author Lucas Satabin
 */
class CouchIndexer(
    val hostname: String = "localhost",
    val port: Int = 5984,
    val ssl: Boolean = false)(implicit val system: ActorSystem,
        timeout: Timeout) {

  // the client instance to communicate with the databases
  private val _client = new CouchClient(hostname, port, ssl)

  import _client.ec

  private val _indexes = mu.Map.empty[String, Index]

  /** Returns the list of current index names */
  def indexes: List[String] =
    _indexes.keySet.toList

  /** @param database The database name for which one wants to build an index
   *  @param indexName The name of this index. If none is given it will be the name of the database.
   *         If several databases share the same index name, only one index with merged indexed results
   *         is created for them
   *  @param indexedFields The field name that mus be indexed in documents. It his is left empty, only the
   *         identifier of a document is indexed.
   *  @param update Tells whether to update the existing index or to clean it up before indexing
   *  @param initialIndex Specify whether the index must be built when the index is created or if it is only
   *         built incrementally when documents are modified. By default it is buil from scratch on creation
   */
  def createIndexFor(database: String,
    indexName: Option[String] = None,
    indexedFields: List[String] = Nil,
    update: Boolean = true,
    initialIndex: Boolean = true,
    credentials: Option[CouchCredentials] = None): Unit = {

      for(client <- authenticated(credentials)) {
        val changes = client.database(database).changes(since = if(initialIndex) Some(0) else None)
      }


  }

  private def authenticated(credentials: Option[CouchCredentials]) = credentials match {
    case Some(credentials) => _client.withCredentials(credentials)
    case None              => Future.successful(_client)
  }

}


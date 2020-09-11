package org.sunbird.actors

import java.util

import akka.actor.Props
import org.apache.commons.lang3.StringUtils
import org.scalamock.scalatest.MockFactory
import org.sunbird.common.dto.Request
import org.sunbird.common.exception.ResponseCode
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import org.sunbird.graph.dac.model.Node
import org.sunbird.utils.Constants

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ObjectCategoryDefinitionActorTest extends BaseSpec with MockFactory {

	"ObjectCategoryDefinitionActor" should "return failed response for 'unknown' operation" in {
		implicit val oec: OntologyEngineContext = new OntologyEngineContext
		testUnknownOperation(Props(new ObjectCategoryDefinitionActor()), getCategoryDefintionRequest())
	}

	it should "create a categoryDefinition node and store it in neo4j" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val graphDB = mock[GraphService]
		(oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
		val node = new Node()
		node.setIdentifier("obj-cat:1234")
		node.setMetadata(new util.HashMap[String, AnyRef]() {
			{
				put("identifier", "obj-cat:1234");
				put("objectType", "ObjectCategory")
			}
		})
		(graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
		(graphDB.addNode(_: String, _: Node)).expects(*, *).returns(Future(getCategoryDefinitionNode()))
		val request = getCategoryDefintionRequest()
		val objectMetadata = new util.HashMap[String, AnyRef](){{
			put("schema", new util.HashMap())
			put("config", new util.HashMap())
		}}
		request.putAll(mapAsJavaMap(Map("name" -> "Test Category Definition", "targetObjectType" -> "Content", "categoryId" -> "obj-cat:1234", "objectMetadata" -> objectMetadata)))
		request.setOperation(Constants.CREATE_OBJECT_CATEGORY_DEFINITION)
		val response = callActor(request, Props(new ObjectCategoryDefinitionActor()))
		assert(response.get(Constants.IDENTIFIER) != null)
		assert(response.get(Constants.IDENTIFIER).equals("obj-cat:1234_content_all"))
	}

	it should "should throw exception if get category node returns null" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val graphDB = mock[GraphService]
		(oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
		(graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(null))
		val request = getCategoryDefintionRequest()
		request.putAll(mapAsJavaMap(Map("name" -> "Test Category Definition", "targetObjectType" -> "Content", "categoryId" -> "obj-cat:1234", "objectMetadata" -> Map("schema" -> Map()), "config" -> Map())))
		request.setOperation(Constants.CREATE_OBJECT_CATEGORY_DEFINITION)
		val response = callActor(request, Props(new ObjectCategoryDefinitionActor()))
		assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
		assert(StringUtils.equalsIgnoreCase(response.getParams.getErrmsg, "Please provide valid category identifier"))
	}

	it should "should throw exception for blank categoryId" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val request = getCategoryDefintionRequest()
		request.putAll(mapAsJavaMap(Map("name" -> "Test Category Definition", "tagetObjectType" -> "Content", "categoryId" -> "")))
		request.setOperation(Constants.CREATE_OBJECT_CATEGORY_DEFINITION)
		val response = callActor(request, Props(new ObjectCategoryDefinitionActor()))
		assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
		assert(StringUtils.equalsIgnoreCase(response.getParams.getErrmsg, "Invalid Request. Please Provide Required Properties!"))
	}

	it should "return success response for readCategoryDefinition" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val graphDB = mock[GraphService]
		(oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
		val node = getCategoryDefinitionNode()
		(graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
		val request = getCategoryDefintionRequest()
		request.getContext.put(Constants.IDENTIFIER, "obj-cat:1234_content_all")
		request.putAll(mapAsJavaMap(Map("fields" -> "")))
		request.setOperation(Constants.READ_OBJECT_CATEGORY_DEFINITION)
		val response = callActor(request, Props(new ObjectCategoryDefinitionActor()))
		val objectCategoryDefinition = response.getResult.getOrDefault("objectCategoryDefinition", new util.HashMap[String, AnyRef]()).asInstanceOf[util.Map[String, AnyRef]]
		assert("successful".equals(response.getParams.getStatus))
		assert("obj-cat:1234_content_all".equals(objectCategoryDefinition.getOrDefault("identifier", "")))
	}

	it should "return success response for updateCategoryDefinition for valid input" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val graphDB = mock[GraphService]
		(oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
		val node = new Node()
		node.setIdentifier("obj-cat:1234_content_all")
		node.setMetadata(new util.HashMap[String, AnyRef]() {
			{
				put("identifier", "obj-cat:1234_content_all")
				put("categoryId", "obj-cat:1234")
				put("objectType", "ObjectCategoryDefinition")
				put("name", "Test Category Definition")
				put("targetObjectType", "Content")
				put("objectMetadata", "{\"schema\":{},\"config\":{}}")
			}
		})
		(graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
		(graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(getCategoryDefinitionNode()))
		val request = getCategoryDefintionRequest()
		request.getContext.put(Constants.IDENTIFIER, "obj-cat:1234_content_all")
		request.putAll(mapAsJavaMap(Map("description" -> "test desc")))
		request.setOperation(Constants.UPDATE_OBJECT_CATEGORY_DEFINITION)
		val response = callActor(request, Props(new ObjectCategoryDefinitionActor()))
		assert("successful".equals(response.getParams.getStatus))
	}

	it should "return client exception response for updateCategoryDefinition for invalid input" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val graphDB = mock[GraphService]
		(oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
		val node = new Node()
		node.setIdentifier("obj-cat:1234_content_all")
		node.setMetadata(new util.HashMap[String, AnyRef]() {
			{
				put("identifier", "obj-cat:1234_content_all")
				put("categoryId", "obj-cat:1234")
				put("objectType", "ObjectCategoryDefinition")
				put("name", "Test Category Definition")
				put("targetObjectType", "Content")
				put("objectMetadata", "{\"schema\":{},\"config\":{}}")
			}
		})
		val request = getCategoryDefintionRequest()
		request.getContext.put(Constants.IDENTIFIER, "obj-cat:1234_content_all")
		request.putAll(mapAsJavaMap(Map("description" -> "test desc", "categoryId" -> "obj-cat:test-1234", "channel" -> "abc")))
		request.setOperation(Constants.UPDATE_OBJECT_CATEGORY_DEFINITION)
		val response = callActor(request, Props(new ObjectCategoryDefinitionActor()))
		assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
		assert(StringUtils.equalsIgnoreCase(response.getParams.getErrmsg, "Properties in list [identifier, categoryId, targetObjectType, channel, status, objectType] are not allowed in request"))
	}

	private def getCategoryDefintionRequest(): Request = {
		val request = new Request()
		request.setContext(getContext())
		request
	}

	private def getContext(): util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]() {
		{
			put("graph_id", "domain")
			put("version", "1.0")
			put("objectType", "ObjectCategoryDefinition")
			put("schemaName", "objectcategorydefinition")

		}
	}

	private def getCategoryDefinitionNode(): Node = {
		val node = new Node()
		node.setIdentifier("obj-cat:1234_content_all")
		node.setNodeType("DATA_NODE")
		node.setMetadata(new util.HashMap[String, AnyRef]() {
			{
				put("identifier", "obj-cat:1234_content_all")
				put("categoryId", "obj-cat:1234")
				put("objectType", "ObjectCategoryDefinition")
				put("name", "Test Category Definition")
				put("targetObjectType", "Content")
				put("objectMetadata", new util.HashMap[String, AnyRef]() {
					{
						put("config", new util.HashMap())
						put("schema", new util.HashMap())
					}
				})
			}
		})
		node
	}
}
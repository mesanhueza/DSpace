package org.dspace.app.xmlui.aspect.ELProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.IteratorUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;

public class MetadataResolver extends Resolver{
	
	private String schema;
	private String element;
	private String qualifier;
	private List<List<MetadataField>> listFieldList = new ArrayList<List<MetadataField>>();
	private List<String> queryOP = new ArrayList<String>();
	private List<String> valueList = new ArrayList<String>();

	protected static final MetadataFieldService metadataService= ContentServiceFactory.getInstance().getMetadataFieldService();
	
	
	public MetadataField getMetadataFieldFromString(String metadata) throws Exception{
		this.prepareMetadataField(metadata);
		return metadataService.findByElement(TransactionManager.getContext(), schema, element, qualifier);
	}
	
	public void getItemsFromMetadataAndValue(List<Condition> conditions, List<UUID> collectionsUUIDs) throws Exception{		
		
		this.processConditions(conditions);

		Iterator<Item> items = itemService.findByMetadataQuery(TransactionManager.getContext(), listFieldList, queryOP, valueList, collectionsUUIDs, "", 0, 0);
		List<Item> result  = IteratorUtils.toList(items);
		
		ResultContainer.addItems(result);
	}
	
	public void getCollectionsFromMetadataAndValue(List<Condition> conditions, List<UUID> collectionsUUIDs) throws Exception{		
		
		this.processConditions(conditions);
		
		Iterator<Collection> collections = collectionService.findByMetadataQuery(TransactionManager.getContext(), listFieldList, queryOP, valueList, "", 0, 0);
		List<Collection> result = IteratorUtils.toList(collections);
		
		ResultContainer.addCollections(result);
	}
	
	public void getCommunitiesFromMetadataAndValue(List<Condition> conditions, List<UUID> collectionsUUIDs) throws Exception{		
		
		this.processConditions(conditions);
		
		Iterator<Community> communities = communityService.findByMetadataQuery(TransactionManager.getContext(), listFieldList, queryOP, valueList, "", 0, 0);
		List<Community> result = IteratorUtils.toList(communities);
		
		ResultContainer.addCommunities(result);
	}
	
	private void processConditions(List<Condition> conditions) throws Exception{
		for(Condition condition: conditions){
			List<MetadataField> fieldList = new ArrayList<MetadataField>();
			fieldList.add(condition.getMetadataField());
			queryOP.add(condition.getOperation());
			valueList.add(condition.getMetadataValue());
			listFieldList.add(fieldList);
		}
	}
	
	private void prepareMetadataField(String value) throws Exception{
		
		String[] arrayMetadataField=value.split("\\.");
		if(arrayMetadataField.length != 2 && arrayMetadataField.length != 3 ){  // chequeo que por lo menos tenga schema y element
			throw new Exception("The metadata has a wrong format");
		}
		schema = arrayMetadataField[0].trim();
		element = arrayMetadataField[1].trim();
		qualifier = null;
		if(arrayMetadataField.length == 3){
			qualifier = arrayMetadataField[2].trim(); 
		}
	}
}

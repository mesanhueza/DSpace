package org.dspace.app.xmlui.aspect.ELProcessor;

public class ConditionForDeleteResolver extends ConditionForUpdateResolver{
	
	@Override
	public void checkConditions(String oneCondition, MetadataResolver mr) throws Exception{
		AddDeleteCondition.createCondition(oneCondition, this, mr);
	}
	
	
}

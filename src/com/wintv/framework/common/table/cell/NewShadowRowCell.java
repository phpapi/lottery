package com.wintv.framework.common.table.cell;
import org.apache.commons.lang.StringUtils;
import org.ecside.core.TableModel;
import org.ecside.core.bean.Column;
import org.ecside.table.cell.Cell;
import org.ecside.util.HtmlBuilder;
import org.ecside.view.html.ColumnBuilder;

public class NewShadowRowCell implements Cell
{
    public String getHtmlDisplay(TableModel model, Column column) {
	column.setEditable(Boolean.FALSE);
	column.setSortable(Boolean.FALSE);
	ColumnBuilder columnBuilder = new ColumnBuilder(column);
	columnBuilder.tdStart();
	columnBuilder.tdBody(getCellValue(model, column));
	columnBuilder.tdEnd();
	return columnBuilder.toString();
    }
    
    public String getExportDisplay(TableModel model, Column column) {
	return column.getPropertyValueAsString();
    }
    
    protected String getCellValue(TableModel model, Column column) {
		HtmlBuilder html = new HtmlBuilder();
		if(!(StringUtils.isNotBlank(column.getCellValue()) && column.getCellValue().equals("false"))){
			html.span().id
		    (new StringBuilder("srb_").append
			 (model.getRowHandler().getRow().getId()).toString())
		    .styleClass("shadowRowButtonClose");
			html.onclick
		    (new StringBuilder
			 ("ECSideUtil.showShadowRow(this.parentNode.parentNode,this,'")
			 .append
			 (model.getTable().getTableId()).append
			 ("');").toString());
			html.close();
			html.append("&#160;");
			html.spanEnd();
		}
		return html.toString();
    }
}
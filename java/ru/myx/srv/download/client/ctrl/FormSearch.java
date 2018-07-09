/*
 * Created on 24.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client.ctrl;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseHostLookup;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.ControlLookupStatic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Convert;
import ru.myx.srv.download.client.DownloadClient;
import ru.myx.srv.download.client.RecFile;

/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class FormSearch extends AbstractForm<FormSearch> {
	
	private static final BaseObject STR_NAME = MultivariantString.getString("Name", Collections.singletonMap("ru", "Имя"));
	
	private static final BaseObject STR_ALL = MultivariantString.getString("Accessibility", Collections.singletonMap("ru", "Доступность"));
	
	private static final BaseObject STR_TYPE = MultivariantString.getString("Type", Collections.singletonMap("ru", "Тип"));
	
	private static final BaseObject STR_INFO = MultivariantString.getString("Info", Collections.singletonMap("ru", "Информация"));
	
	private static final BaseObject STR_LINK = MultivariantString.getString("Link", Collections.singletonMap("ru", "Ссылка"));
	
	private static final BaseObject STR_RESULTS = MultivariantString.getString("Search results", Collections.singletonMap("ru", "Результаты поиска"));
	
	private static final BaseHostLookup LOOKUP_ALL = new ControlLookupStatic()
			.putAppend("false", MultivariantString.getString("Only accessible", Collections.singletonMap("ru", "Только доступные")))
			.putAppend("true", MultivariantString.getString("Any type of accessibility", Collections.singletonMap("ru", "Любой тип доступности")));
			
	private static final BaseHostLookup LOOKUP_TYPE = new ControlLookupStatic()
			.putAppend("-", MultivariantString.getString("Any type", Collections.singletonMap("ru", "Любого типа")))
			.putAppend("audio", MultivariantString.getString("Audio", Collections.singletonMap("ru", "Аудио")))
			.putAppend("image", MultivariantString.getString("Image", Collections.singletonMap("ru", "Изображение")))
			.putAppend("text", MultivariantString.getString("Text", Collections.singletonMap("ru", "Текст")));
			
	private static final ControlFieldset<?> FIELDSET_LISTING = ControlFieldset.createFieldset()
			.addField(ControlFieldFactory.createFieldString("name", FormSearch.STR_NAME, "")).addField(ControlFieldFactory.createFieldString("type", FormSearch.STR_TYPE, ""))
			.addField(ControlFieldFactory.createFieldString("info", FormSearch.STR_INFO, ""))
			.addField(ControlFieldFactory.createFieldString("link", FormSearch.STR_LINK, "").setFieldVariant("link"));
			
	private static final ControlFieldset<?> FIELDSET = ControlFieldset.createFieldset()
			.addField(ControlFieldFactory.createFieldString("text", FormSearch.STR_NAME, "", 1, 255))
			.addField(ControlFieldFactory.createFieldBoolean("all", FormSearch.STR_ALL, false).setFieldType("select").setAttribute("lookup", FormSearch.LOOKUP_ALL))
			.addField(ControlFieldFactory.createFieldString("type", FormSearch.STR_TYPE, "-").setFieldType("select").setAttribute("lookup", FormSearch.LOOKUP_TYPE))
			.addField(Control.createFieldList("result", FormSearch.STR_RESULTS, null).setConstant().setAttribute("content_fieldset", FormSearch.FIELDSET_LISTING));
			
	private static final BaseObject STR_TITLE = MultivariantString.getString("Search for files", Collections.singletonMap("ru", "Поиск файлов"));
	
	private final DownloadClient parent;
	
	private static final BaseObject STR_SEARCH = MultivariantString.getString("Search", Collections.singletonMap("ru", "Искать"));
	
	private static final ControlCommand<?> CMD_SEARCH = Control.createCommand("search", FormSearch.STR_SEARCH).setCommandIcon("command-search");
	
	FormSearch(final DownloadClient parent) {
		this.parent = parent;
		this.setAttributeIntern("id", "search");
		this.setAttributeIntern("title", FormSearch.STR_TITLE);
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		
		if (command == FormSearch.CMD_SEARCH) {
			final String search = Base.getString(this.getData(), "text", "").trim();
			final boolean all = Convert.MapEntry.toBoolean(this.getData(), "all", false);
			final String type = Base.getString(this.getData(), "type", "-").trim();
			final RecFile[] files = this.parent.searchByName(search, type.length() <= 1
				? ""
				: type, all);
			final BaseArrayDynamic<ControlBasic<?>> result = BaseObject.createArray();
			for (final RecFile current : files) {
				final String guid = current.getGuid();
				final String name = current.getName();
				final BaseObject data = new BaseNativeObject()//
						.putAppend("name", name)//
						.putAppend("type", current.getType())//
						.putAppend("info", current.getComment())//
						.putAppend("link", current.getPath())//
						;
				result.add(Control.createBasic(guid, name, data));
			}
			this.getData().baseDefine("result", result);
			return this;
		}
		return super.getCommandResult(command, arguments);
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		final ControlCommandset result = Control.createOptions();
		result.add(FormSearch.CMD_SEARCH);
		return result;
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		
		return FormSearch.FIELDSET;
	}
}

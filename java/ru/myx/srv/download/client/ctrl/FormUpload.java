package ru.myx.srv.download.client.ctrl;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseHostLookup;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlLookupStatic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Convert;
import ru.myx.srv.download.client.DownloadClient;
import ru.myx.srv.download.client.RecSource;

/*
 * Created on 20.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class FormUpload extends AbstractForm<FormUpload> {
	
	private static final BaseObject STR_TITLE = MultivariantString.getString("Server registration", Collections.singletonMap("ru", "Регистрация сервера"));
	
	private static final BaseObject STR_HOST = MultivariantString.getString("Host name", Collections.singletonMap("ru", "Имя хоста"));
	
	private static final BaseObject STR_HOST_HINT = MultivariantString
			.getString("Enter a dns server name or an IP address", Collections.singletonMap("ru", "Введите dns-имя хоста или его IP адрес"));
			
	private static final BaseObject STR_PORT = MultivariantString.getString("Host port", Collections.singletonMap("ru", "Порт сервера"));
	
	private static final BaseObject STR_PORT_HINT = MultivariantString.getString("Enter port number, usually 80", Collections.singletonMap("ru", "Введите номер порта, обычно 80"));
	
	private static final BaseObject STR_IDX_HOST = MultivariantString.getString("Indexing host", Collections.singletonMap("ru", "Сервер для индексирования"));
	
	private static final BaseObject STR_IDX_PORT = MultivariantString.getString("Indexing port", Collections.singletonMap("ru", "Порт для индексирования"));
	
	private static final BaseObject STR_INDEXING = MultivariantString.getString("Indexing", Collections.singletonMap("ru", "Индексировать"));
	
	private static final BaseObject STR_INDEXING_HINT = MultivariantString
			.getString("File indexation mode", Collections.singletonMap("ru", "Разрешить индексацию файлов с этого сервера"));
			
	private static final BaseObject STR_USAGE = MultivariantString.getString("Usage", Collections.singletonMap("ru", "Использование"));
	
	private static final BaseObject STR_USAGE_HINT = MultivariantString.getString("Download server usage mode", Collections.singletonMap("ru", "Режим использования сервера"));
	
	private static final BaseHostLookup LOOKUP_INDEXING_MODE = new ControlLookupStatic()
			.putAppend("true", MultivariantString.getString("Do index this server files", Collections.singletonMap("ru", "Индексировать файлы с этого сервера")))
			.putAppend("false", MultivariantString.getString("Do NOT index this server files", Collections.singletonMap("ru", "НЕ индексировать файлы с этого сервера")));
			
	private static final BaseHostLookup LOOKUP_USAGE_MODE = new ControlLookupStatic().putAppend(
			"true",
			MultivariantString.getString("Use this server files while searching for links", Collections.singletonMap("ru", "Использовать сервер при поиске файлов"))).putAppend(
					"false",
					MultivariantString
							.getString("DO NOT use this server files while searching for links", Collections.singletonMap("ru", "НЕ использовать сервер при поиске файлов")));
							
	private static final ControlFieldset<?> FIELDSET = ControlFieldset.createFieldset()
			.addField(ControlFieldFactory.createFieldString("srchost", FormUpload.STR_HOST, "", 1, 255).setFieldHint(FormUpload.STR_HOST_HINT))
			.addField(ControlFieldFactory.createFieldInteger("srcport", FormUpload.STR_PORT, 80).setFieldHint(FormUpload.STR_PORT_HINT))
			.addField(ControlFieldFactory.createFieldString("idxhost", FormUpload.STR_IDX_HOST, "", 1, 255).setFieldHint(FormUpload.STR_HOST_HINT))
			.addField(ControlFieldFactory.createFieldInteger("idxport", FormUpload.STR_IDX_PORT, 80).setFieldHint(FormUpload.STR_PORT_HINT))
			.addField(
					ControlFieldFactory.createFieldBoolean("indexing", FormUpload.STR_INDEXING, false).setFieldType("select").setFieldVariant("bigselect")
							.setFieldHint(FormUpload.STR_INDEXING_HINT).setAttribute("lookup", FormUpload.LOOKUP_INDEXING_MODE))
			.addField(
					ControlFieldFactory.createFieldBoolean("usage", FormUpload.STR_USAGE, false).setFieldType("select").setFieldVariant("bigselect")
							.setFieldHint(FormUpload.STR_USAGE_HINT).setAttribute("lookup", FormUpload.LOOKUP_USAGE_MODE));
							
	private static final BaseObject STR_IDENTITY = MultivariantString.getString("Identity", Collections.singletonMap("ru", "Идентификатор"));
	
	private static final BaseObject STR_VERSION = MultivariantString.getString("Version", Collections.singletonMap("ru", "Версия"));
	
	private static final BaseObject STR_HEALTH = MultivariantString.getString("Health", Collections.singletonMap("ru", "Здоровье"));
	
	private static final BaseObject STR_READY = MultivariantString.getString("Ready", Collections.singletonMap("ru", "Готовность"));
	
	private static final ControlFieldset<?> FIELDSET_CHECKED = ControlFieldset.createFieldset()
			.addField(ControlFieldFactory.createFieldString("srchost", FormUpload.STR_HOST, "", 1, 255).setConstant())
			.addField(ControlFieldFactory.createFieldInteger("srcport", FormUpload.STR_PORT, 80).setConstant())
			.addField(ControlFieldFactory.createFieldString("idxhost", FormUpload.STR_IDX_HOST, "", 1, 255).setConstant())
			.addField(ControlFieldFactory.createFieldInteger("idxport", FormUpload.STR_IDX_PORT, 80).setConstant())
			.addField(
					ControlFieldFactory.createFieldBoolean("indexing", FormUpload.STR_INDEXING, false).setFieldType("select").setFieldVariant("bigselect").setConstant()
							.setAttribute("lookup", FormUpload.LOOKUP_INDEXING_MODE))
			.addField(
					ControlFieldFactory.createFieldBoolean("usage", FormUpload.STR_USAGE, false).setFieldType("select").setFieldVariant("bigselect").setConstant()
							.setAttribute("lookup", FormUpload.LOOKUP_USAGE_MODE))
			.addField(ControlFieldFactory.createFieldString("identity", FormUpload.STR_IDENTITY, "-").setConstant())
			.addField(ControlFieldFactory.createFieldString("version", FormUpload.STR_VERSION, "-").setConstant())
			.addField(ControlFieldFactory.createFieldString("health", FormUpload.STR_HEALTH, "-").setConstant())
			.addField(ControlFieldFactory.createFieldString("ready", FormUpload.STR_READY, "-").setConstant());
			
	private final DownloadClient parent;
	
	private static final BaseObject STR_TEST = MultivariantString.getString("Test it", Collections.singletonMap("ru", "Проверить сервер"));
	
	private static final ControlCommand<?> CMD_TEST = Control.createCommand("test", FormUpload.STR_TEST).setCommandPermission("create").setCommandIcon("command-next");
	
	static final ControlFieldset<?> getPropertiesFieldset() {
		
		return FormUpload.FIELDSET;
	}
	
	FormUpload(final DownloadClient parent) {
		this.parent = parent;
		this.setAttributeIntern("id", "upload");
		this.setAttributeIntern("title", FormUpload.STR_TITLE);
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		
		if (command == FormUpload.CMD_TEST) {
			final String srcHost = Base.getString(this.getData(), "srchost", "").trim();
			final int srcPort = Convert.MapEntry.toInt(this.getData(), "srcport", 80);
			final String idxHost = Base.getString(this.getData(), "idxhost", "").trim();
			final int idxPort = Convert.MapEntry.toInt(this.getData(), "idxport", 80);
			final RecSource source = new RecSource(
					this.parent,
					-1,
					"",
					srcHost,
					srcPort,
					idxHost,
					idxPort,
					-1L,
					-1L,
					Convert.MapEntry.toBoolean(this.getData(), "indexing", false),
					Convert.MapEntry.toBoolean(this.getData(), "usage", false));
			try {
				source.check();
				final BaseObject data = new BaseNativeObject();
				data.baseDefineImportAllEnumerable(this.getData());
				data.baseDefine("identity", source.getGuid());
				data.baseDefine("version", source.getVersion());
				data.baseDefine("health", Math.round(source.getHealth() * 100) + "%");
				data.baseDefine("ready", Math.round(source.getReady() * 100) + "%");
				return new FormCreate2(this, this.parent, source, data, FormUpload.FIELDSET_CHECKED);
			} catch (final Throwable e) {
				return new FormCreate2(this, null, null, this.getData(), FormUpload.FIELDSET_CHECKED);
			}
		}
		return super.getCommandResult(command, arguments);
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		final ControlCommandset result = Control.createOptions();
		result.add(FormUpload.CMD_TEST);
		return result;
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		
		return FormUpload.FIELDSET;
	}
}

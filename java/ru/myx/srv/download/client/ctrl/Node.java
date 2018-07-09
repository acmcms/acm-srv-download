/*
 * Created on 20.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.myx.ae1.control.AbstractNode;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.ControlNode;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.srv.download.client.DownloadClient;
import ru.myx.srv.download.client.RecFolder;
import ru.myx.srv.download.client.RecSource;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class Node extends AbstractNode {
	private static final BaseObject		STR_TITLE			= MultivariantString.getString( "Download client",
																		Collections.singletonMap( "ru",
																				"Клиент сервера скачки" ) );
	
	private static final BaseObject		STR_REGISTER_SERVER	= MultivariantString
																		.getString( "New server registration",
																				Collections.singletonMap( "ru",
																						"Регистрация сервера" ) );
	
	private static final ControlCommand<?>	CMD_REGISTER_SERVER	= Control
																		.createCommand( "register",
																				Node.STR_REGISTER_SERVER )
																		.setCommandPermission( "create" )
																		.setCommandIcon( "command-create" );
	
	private static final BaseObject		STR_SEARCH			= MultivariantString
																		.getString( "Search files", Collections
																				.singletonMap( "ru", "Поиск файлов" ) );
	
	private static final ControlCommand<?>	CMD_SEARCH			= Control.createCommand( "search", Node.STR_SEARCH )
																		.setCommandPermission( "view" )
																		.setCommandIcon( "command-search" );
	
	private static final BaseObject		STR_IDENTITY		= MultivariantString.getString( "Identity", Collections
																		.singletonMap( "ru", "Идентификатор" ) );
	
	private static final BaseObject		STR_HOST			= MultivariantString.getString( "Host",
																		Collections.singletonMap( "ru", "Хост" ) );
	
	private static final BaseObject		STR_PORT			= MultivariantString.getString( "Port",
																		Collections.singletonMap( "ru", "Порт" ) );
	
	private static final BaseObject		STR_INDEX			= MultivariantString.getString( "Index", Collections
																		.singletonMap( "ru", "Индексировать" ) );
	
	private static final BaseObject		STR_ACTIVE			= MultivariantString.getString( "Active",
																		Collections.singletonMap( "ru", "Активный" ) );
	
	private static final BaseObject		STR_CHECKED			= MultivariantString.getString( "Checked",
																		Collections.singletonMap( "ru", "Проверялся" ) );
	
	private static final BaseObject		STR_COUNT			= MultivariantString.getString( "Count",
																		Collections.singletonMap( "ru", "Количество" ) );
	
	private static final ControlFieldset<?>	FIELDSET_LISTING	= ControlFieldset
																		.createFieldset()
																		.addField( ControlFieldFactory
																				.createFieldString( "host",
																						Node.STR_HOST,
																						"" ) )
																		.addField( ControlFieldFactory
																				.createFieldInteger( "port",
																						Node.STR_PORT,
																						0 ) )
																		.addField( ControlFieldFactory
																				.createFieldString( "identity",
																						Node.STR_IDENTITY,
																						"" ) )
																		.addField( ControlFieldFactory
																				.createFieldBoolean( "index",
																						Node.STR_INDEX,
																						false ) )
																		.addField( ControlFieldFactory
																				.createFieldBoolean( "active",
																						Node.STR_ACTIVE,
																						false ) )
																		.addField( ControlFieldFactory.createFieldDate( "checked",
																				Node.STR_CHECKED,
																				0L ) )
																		.addField( ControlFieldFactory
																				.createFieldInteger( "count",
																						Node.STR_COUNT,
																						0 ) );
	
	private static final BaseObject		STR_EDIT			= MultivariantString.getString( "Properties",
																		Collections.singletonMap( "ru", "Свойства" ) );
	
	private static final BaseObject		STR_DELETE			= MultivariantString.getString( "Delete",
																		Collections.singletonMap( "ru", "УДалить" ) );
	
	private final DownloadClient			parent;
	
	/**
	 * @param parent
	 */
	public Node(final DownloadClient parent) {
		this.parent = parent;
		this.setAttributeIntern( "id", "dlc1" );
		this.setAttributeIntern( "title", Node.STR_TITLE );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		if (command == Node.CMD_REGISTER_SERVER) {
			return new FormCreate1( this.parent );
		}
		if (command == Node.CMD_SEARCH) {
			return new FormSearch( this.parent );
		}
		if (command.getKey().equals( "edit" )) {
			final String key = Base.getString( command.getAttributes(), "key", "" ).trim();
			return new FormEdit( this.parent, this.parent.getSource( key ) );
		}
		if (command.getKey().equals( "delete" )) {
			final String key = Base.getString( command.getAttributes(), "key", "" ).trim();
			return new FormDelete( this.getLocationControl(), this.parent, this.parent.getSource( key ) );
		}
		return super.getCommandResult( command, arguments );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( Node.CMD_SEARCH );
		result.add( Node.CMD_REGISTER_SERVER );
		return result;
	}
	
	@Override
	public ControlCommandset getContentCommands(final String key) {
		final ControlCommandset result = Control.createOptions();
		result.add( Control.createCommand( "edit", Node.STR_EDIT ).setCommandPermission( "edit" )
				.setCommandIcon( "command-edit" ).setAttribute( "key", key ) );
		result.add( Control.createCommand( "delete", Node.STR_DELETE ).setCommandPermission( "delete" )
				.setCommandIcon( "command-delete" ).setAttribute( "key", key ) );
		return result;
	}
	
	@Override
	public ControlFieldset<?> getContentFieldset() {
		return Node.FIELDSET_LISTING;
	}
	
	@Override
	public List<ControlBasic<?>> getContents() {
		final List<ControlBasic<?>> result = new ArrayList<>();
		final RecSource[] sources = this.parent.getSources( true );
		for (int i = sources.length - 1; i >= 0; --i) {
			final BaseObject data = new BaseNativeObject()//
					.putAppend( "identity", sources[i].getGuid() )//
					.putAppend( "host", sources[i].getHost() )//
					.putAppend( "port", sources[i].getPort() )//
					.putAppend( "index", sources[i].isIndex() )//
					.putAppend( "active", sources[i].isActive() )//
					.putAppend( "checked", Base.forDateMillis( sources[i].getChecked() ) )//
					.putAppend( "count", this.parent.getSourceFileCount( sources[i].getGuid() ) )//
			;
			result.add( Control.createBasic( sources[i].getGuid(), sources[i].getHost(), data ) );
		}
		return result;
	}
	
	@Override
	public final String getLocationControl() {
		return '/' + this.getKey();
	}
	
	@Override
	protected ControlNode<?> internGetChildByName(final String name) {
		final RecSource source = this.parent.getSource( name );
		if (source == null) {
			return null;
		}
		final RecFolder folder = source.getRootFolder();
		if (folder == null) {
			return null;
		}
		return new NodeFolder( this.parent, source.getRootFolder() );
	}
	
	@Override
	protected ControlNode<?>[] internGetChildren() {
		final RecSource[] sources = this.parent.getSources( true );
		if (sources == null || sources.length == 0) {
			return null;
		}
		final List<ControlNode<?>> result = new ArrayList<>( sources.length );
		for (final RecSource source : sources) {
			final RecFolder folder = source.getRootFolder();
			if (folder == null) {
				return null;
			}
			result.add( new NodeFolder( this.parent, folder ) );
		}
		return result.toArray( new ControlNode<?>[result.size()] );
	}
	
	@Override
	protected boolean internHasChildren() {
		final RecSource[] sources = this.parent.getSources( true );
		return sources != null && sources.length > 0;
	}
}

/*
 * Created on 20.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client.ctrl;

import java.sql.SQLException;
import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.srv.download.client.DownloadClient;
import ru.myx.srv.download.client.RecSource;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class FormCreate2 extends AbstractForm<FormCreate2> {
	private static final BaseObject		STR_TITLE_FAILED	= MultivariantString.getString( "Test failed",
																		Collections.singletonMap( "ru",
																				"Тест провалился" ) );
	
	private static final BaseObject		STR_TITLE_SUCCEED	= MultivariantString.getString( "Test succeed",
																		Collections.singletonMap( "ru",
																				"Протестировано успешно" ) );
	
	private final ControlForm<?>			backForm;
	
	private final DownloadClient			parent;
	
	private final RecSource					source;
	
	private final ControlFieldset<?>		fieldset;
	
	private static final Object				STR_BACK			= MultivariantString.getString( "Back",
																		Collections.singletonMap( "ru", "Назад" ) );
	
	private static final ControlCommand<?>	CMD_BACK			= Control.createCommand( "back", FormCreate2.STR_BACK )
																		.setCommandPermission( "create" )
																		.setCommandIcon( "command-back" );
	
	private static final Object				STR_REGISTER		= MultivariantString.getString( "Register", Collections
																		.singletonMap( "ru", "Зарегистрировать" ) );
	
	private static final ControlCommand<?>	CMD_REGISTER		= Control
																		.createCommand( "register",
																				FormCreate2.STR_REGISTER )
																		.setCommandPermission( "create" )
																		.setCommandIcon( "command-create" );
	
	FormCreate2(final ControlForm<?> backForm,
			final DownloadClient parent,
			final RecSource source,
			final BaseObject data,
			final ControlFieldset<?> fieldset) {
		this.backForm = backForm;
		this.parent = parent;
		this.source = source;
		this.fieldset = fieldset;
		this.setData( data );
		this.setAttributeIntern( "id", "create2" );
		this.setAttributeIntern( "title", parent == null
				? FormCreate2.STR_TITLE_FAILED
				: FormCreate2.STR_TITLE_SUCCEED );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		if (command == FormCreate2.CMD_BACK) {
			return this.backForm;
		}
		if (command == FormCreate2.CMD_REGISTER) {
			try {
				this.parent.addSource( this.source.getGuid(),
						this.source.getHost(),
						this.source.getPort(),
						this.source.getHostIndexing(),
						this.source.getPortIndexing(),
						this.source.isIndex(),
						this.source.isActive() );
				return null;
			} catch (final SQLException e) {
				throw new RuntimeException( e );
			}
		}
		{
			return super.getCommandResult( command, arguments );
		}
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		if (this.backForm != null) {
			result.add( FormCreate2.CMD_BACK );
		}
		if (this.parent != null) {
			result.add( FormCreate2.CMD_REGISTER );
		}
		return result;
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return this.fieldset;
	}
}

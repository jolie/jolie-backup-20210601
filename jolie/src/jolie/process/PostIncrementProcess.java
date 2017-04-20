/*
 * Copyright (C) 2006-2015 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.process;

import jolie.ExecutionThread;
import jolie.lang.Constants;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.TypeCastingException;

public class PostIncrementProcess implements Process, Expression
{
	private final VariablePath path;

	public PostIncrementProcess( VariablePath varPath )
	{
		this.path = varPath;
	}
	
	@Override
	public Process clone( TransformationReason reason )
	{
		return new PostIncrementProcess( (VariablePath)path.cloneExpression( reason ) );
	}
	
	@Override
	public Expression cloneExpression( TransformationReason reason )
	{
		return new PostIncrementProcess( (VariablePath)path.cloneExpression( reason ) );
	}
	
	@Override
	public void run() throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
		final Value val = path.getValue();
        try {
            val.setValue( val.intValueStrict() + 1 );
        } catch ( TypeCastingException e ){
            throw new FaultException(
                Constants.CASTING_EXCEPTION_FAULT_NAME,
                "Could not increment a non-integer value"
            );
        }
		
	}
	
	@Override
	public Value evaluate() throws FaultException
	{
		final Value val = path.getValue();
		final Value orig = Value.create();
        try {
            orig.setValue( val.intValueStrict() );
            val.setValue( val.intValueStrict() + 1 );
        } catch ( TypeCastingException e ){
            throw new FaultException(
                Constants.CASTING_EXCEPTION_FAULT_NAME,
                "Could not increment a non-integer value"
            );
        }
        
		return orig;
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}

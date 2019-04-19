/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.lang.parse.ast.expression;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.context.ParsingContext;


public class InlineTreeExpressionNode extends OLSyntaxNode
{
	public static interface Operation {}
	
	public static class AssignmentOperation implements Operation {
		private final VariablePathNode path;
		private final OLSyntaxNode expression;
		
		public AssignmentOperation( VariablePathNode path, OLSyntaxNode expression )
		{
			this.path = path;
			this.expression = expression;
		}
		
		public VariablePathNode path()
		{
			return path;
		}
		
		public OLSyntaxNode expression()
		{
			return expression;
		}
	}
	
	public static class DeepCopyOperation implements Operation {
		private final VariablePathNode path;
		private final OLSyntaxNode expression;
		
		public DeepCopyOperation( VariablePathNode path, OLSyntaxNode expression )
		{
			this.path = path;
			this.expression = expression;
		}
		
		public VariablePathNode path()
		{
			return path;
		}
		
		public OLSyntaxNode expression()
		{
			return expression;
		}
	}
	
	public static class PointsToOperation implements Operation {
		private final VariablePathNode path;
		private final VariablePathNode target;
		
		public PointsToOperation( VariablePathNode path, VariablePathNode target )
		{
			this.path = path;
			this.target = target;
		}
		
		public VariablePathNode path()
		{
			return path;
		}
		
		public VariablePathNode target()
		{
			return target;
		}
	}
	
	private final OLSyntaxNode rootExpression;
	private final Operation[] operations;

	public InlineTreeExpressionNode(
		ParsingContext context,
		OLSyntaxNode rootExpression,
		Operation[] operations
	) {
		super( context );
		this.rootExpression = rootExpression;
		this.operations = operations;
	}
	
	public OLSyntaxNode rootExpression()
	{
		return rootExpression;
	}
	
	public Operation[] operations()
	{
		return operations;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}

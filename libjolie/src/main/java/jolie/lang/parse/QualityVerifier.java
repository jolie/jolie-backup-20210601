package jolie.lang.parse;

import jolie.lang.Constants;
import jolie.lang.parse.ast.*;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.types.*;
import jolie.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QualityVerifier implements OLVisitor {

    public class ThrownFault {
        private String faultName;
        private String raisedBy;

        public ThrownFault( String faultName, String raisedBy ) {
            this.faultName = faultName;
            this.raisedBy = raisedBy;
        }

        public String getFaultName() {
            return faultName;
        }

        public String getRaisedBy() {
            return raisedBy;
        }
    }

    public class ScopeTree {

        private ScopeTree parent;
        private ArrayList<ScopeTree> children = new ArrayList<>();
        private HashMap<String, ThrownFault> thrownFaults = new HashMap();
        private ArrayList<String> catchedFaults = new ArrayList<>();

        public ScopeTree( ScopeTree parent ) {
            this.parent = parent;
        }

        public void addChild( ScopeTree scopeTree ) {
            children.add( scopeTree );
        }

        public void addThrownFault( ThrownFault fault) {
            if ( !thrownFaults.containsKey( fault.getFaultName() ) ) {
                thrownFaults.put( fault.getFaultName(), fault );
            }
        }

        public void addCatchedFault( String name ) {
            if ( !catchedFaults.contains( name ) ) {
                catchedFaults.add(name);
            }
        }

        public ScopeTree getParent() {
            return parent;
        }

        public HashMap<String, ThrownFault> getThrownFaultList() {
            return thrownFaults;
        }

        public void checkFaults() {
            for( ScopeTree s : children ) {
                s.checkFaults();
            }


            if ( catchedFaults.contains("default") ) {
                thrownFaults.clear();
            } else {
                for( String catched : catchedFaults ) {
                    if ( thrownFaults.containsKey( catched ) ) {
                        thrownFaults.remove( catched );
                    }
                }
            }

            // promote the remaining faults to the parent scope
            for( Map.Entry<String,ThrownFault> f : thrownFaults.entrySet() ) {
                if ( parent != null ) {
                    parent.addThrownFault(f.getValue());
                }
            }

        }
    }

    private HashMap<String, OutputPortInfo> outputPortInfoMap = new HashMap<>();
    private HashMap<String, InputPortInfo> inputPortInfoMap = new HashMap<>();
    private HashMap<String, ScopeTree> mainScopes = new HashMap<>();
    private HashMap<String, DefinitionNode> defines = new HashMap<>();
    private ScopeTree currentScope;
    private Program program;

    public QualityVerifier( Program program ) {
        this.program = program;
    }

    @Override
    public void visit(Program n) {
        for( OLSyntaxNode node : n.children() ) {
            node.accept( this );
        }
    }

    @Override
    public void visit(OneWayOperationDeclaration decl) {
        // nothing to do
    }

    @Override
    public void visit(RequestResponseOperationDeclaration decl) {
        // nothing to do
    }

    @Override
    public void visit(DefinitionNode n) {
        if ( "main".equals( n.id() ) ) {
            if ( n.body() instanceof  NDChoiceStatement ) {
                for( Pair<OLSyntaxNode,OLSyntaxNode> pair : ((NDChoiceStatement) n.body()).children() ) {
                    ScopeTree newScope = new ScopeTree( null );
                    String operationName;
                    if ( pair.key() instanceof RequestResponseOperationStatement ) {
                        operationName = ((RequestResponseOperationStatement) pair.key()).id();
                    } else {
                        operationName = ((OneWayOperationStatement) pair.key()).id();
                    }
                    mainScopes.put( operationName, newScope );
                    currentScope = newScope;
                    pair.key().accept( this );
                    pair.value().accept( this );
                }

            } else if ( n.body() instanceof RequestResponseOperationStatement ) {
                ScopeTree newScope = new ScopeTree( null );
                String operationName = ((RequestResponseOperationStatement) n.body()).id();
                mainScopes.put( operationName, newScope );
                currentScope = newScope;
                n.body().accept(this );
            } else {
                ScopeTree newScope = new ScopeTree( null );
                mainScopes.put( null, newScope );
                currentScope = newScope;
                n.body().accept(this );
            }
        } else {

            if ( !defines.containsKey( n.id() )) {
                defines.put(n.id(), n);
            }
        }
    }

    @Override
    public void visit(ParallelStatement n) {
        for( OLSyntaxNode node : n.children() ) {
            node.accept( this );
        }
    }

    @Override
    public void visit(SequenceStatement n) {
        for( OLSyntaxNode node : n.children() ) {
            node.accept( this );
        }
    }

    @Override
    public void visit(NDChoiceStatement n) {
        for(Pair<OLSyntaxNode, OLSyntaxNode> pair : n.children() ) {
            pair.key().accept( this );
            pair.value().accept( this );
        }
    }

    @Override
    public void visit(OneWayOperationStatement n) {
        // nothing to do
    }

    @Override
    public void visit(RequestResponseOperationStatement n) {
        ScopeTree parent = currentScope;
        ScopeTree newScope = new ScopeTree( parent );
        parent.addChild( newScope );
        currentScope = newScope;
        n.process().accept(this );
        currentScope = parent;
    }

    @Override
    public void visit(NotificationOperationStatement n) {
        // nothing to do
    }

    @Override
    public void visit(SolicitResponseOperationStatement n) {
        OperationDeclaration operationDeclaration = getOperationDeclarationFromInterface( n.outputPortId(), n.id() );
        if ( operationDeclaration instanceof  RequestResponseOperationDeclaration ) {
            for( Map.Entry<String, TypeDefinition> fault : ((RequestResponseOperationDeclaration) operationDeclaration).faults().entrySet() ) {
                ThrownFault thrownFault = new ThrownFault( fault.getKey(), n.id() + "@" + n.outputPortId() );
                currentScope.addThrownFault( thrownFault );
            }
        }
    }

    @Override
    public void visit(LinkInStatement n) {
        // nothing to do
    }

    @Override
    public void visit(LinkOutStatement n) {
        // nothing to do
    }

    @Override
    public void visit(AssignStatement n) {
        // nothing to do
    }

    @Override
    public void visit(AddAssignStatement n) {
        // nothing to do
    }

    @Override
    public void visit(SubtractAssignStatement n) {
        // nothing to do
    }

    @Override
    public void visit(MultiplyAssignStatement n) {
        // nothing to do
    }

    @Override
    public void visit(DivideAssignStatement n) {
        // nothing to do
    }

    @Override
    public void visit(IfStatement n) {
        for(Pair<OLSyntaxNode, OLSyntaxNode> pair : n.children() ) {
            pair.key().accept( this );
            pair.value().accept( this );
        }
        if ( n.elseProcess() != null ) {
            n.elseProcess().accept(this);
        }
    }

    @Override
    public void visit(DefinitionCallStatement n) {

        if ( defines.get( n.id() ) != null ) {
            defines.get(n.id()).body().accept(this);
        }
    }

    @Override
    public void visit(WhileStatement n) {
        n.body().accept( this );
    }

    @Override
    public void visit(OrConditionNode n) {
        // nothing to do
    }

    @Override
    public void visit(AndConditionNode n) {
        // nothing to do
    }

    @Override
    public void visit(NotExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(CompareConditionNode n) {
        // nothing to do
    }

    @Override
    public void visit(ConstantIntegerExpression n) {
        // nothing to do
    }

    @Override
    public void visit(ConstantDoubleExpression n) {
        // nothing to do
    }

    @Override
    public void visit(ConstantBoolExpression n) {
        // nothing to do
    }

    @Override
    public void visit(ConstantLongExpression n) {
        // nothing to do
    }

    @Override
    public void visit(ConstantStringExpression n) {
        // nothing to do
    }

    @Override
    public void visit(ProductExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(SumExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(VariableExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(NullProcessStatement n) {
        // nothing to do
    }

    @Override
    public void visit(Scope n) {
        ScopeTree parent = currentScope;
        ScopeTree newScope = new ScopeTree( parent );
        parent.addChild( newScope );
        currentScope = newScope;
        n.body().accept( this );
        currentScope = parent;
    }

    @Override
    public void visit(InstallStatement n) {
        for( int i = 0; i < n.handlersFunction().pairs().length; i++ ) {
            currentScope.addCatchedFault( n.handlersFunction().pairs()[ i ].key() );
            // create a fake scope for dealing with processes inside the handlers which could raise faults at the level of the parent
            ScopeTree handlerScope = new ScopeTree( currentScope.getParent() );
            if ( currentScope.getParent() != null ) {
                currentScope.getParent().addChild(handlerScope);
            } else {
                currentScope.addChild(handlerScope);
            }
            ScopeTree tmpScope = currentScope;
            currentScope = handlerScope;
            n.handlersFunction().pairs()[ i ].value().accept( this );
            currentScope = tmpScope;

        }
    }

    @Override
    public void visit(CompensateStatement n) {
         // nothing to do
    }

    @Override
    public void visit(ThrowStatement n) {
        currentScope.addThrownFault( new ThrownFault( n.id(), null ) );
    }

    @Override
    public void visit(ExitStatement n) {
        // nothing to do
    }

    @Override
    public void visit(ExecutionInfo n) {
        // nothing to do
    }

    @Override
    public void visit(CorrelationSetInfo n) {
        // nothing to do
    }

    @Override
    public void visit(InputPortInfo n) {
        inputPortInfoMap.put( n.id(),n );
    }

    @Override
    public void visit(OutputPortInfo n) {
        outputPortInfoMap.put( n.id(), n );
    }

    @Override
    public void visit(PointerStatement n) {
        // nothing to do
    }

    @Override
    public void visit(DeepCopyStatement n) {
        // nothing to do
    }

    @Override
    public void visit(RunStatement n) {
        // nothing to do
    }

    @Override
    public void visit(UndefStatement n) {
        // nothing to do
    }

    @Override
    public void visit(ValueVectorSizeExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(PreIncrementStatement n) {
        // nothing to do
    }

    @Override
    public void visit(PostIncrementStatement n) {
        // nothing to do
    }

    @Override
    public void visit(PreDecrementStatement n) {
        // nothing to do
    }

    @Override
    public void visit(PostDecrementStatement n) {
        // nothing to do
    }

    @Override
    public void visit(ForStatement n) {
        n.body().accept(this );
    }

    @Override
    public void visit(ForEachSubNodeStatement n) {
        n.body().accept( this );
    }

    @Override
    public void visit(ForEachArrayItemStatement n) {
        n.body().accept( this );
    }

    @Override
    public void visit(SpawnStatement n) {
        n.body().accept( this );
    }

    @Override
    public void visit(IsTypeExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(InstanceOfExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(TypeCastExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(SynchronizedStatement n) {
       n.body().accept( this );
    }

    @Override
    public void visit(CurrentHandlerStatement n) {
        // nothing to do
    }

    @Override
    public void visit(EmbeddedServiceNode n) {
        // nothing to do
    }

    @Override
    public void visit(InstallFixedVariableExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(VariablePathNode n) {
        // nothing to do
    }

    @Override
    public void visit(TypeInlineDefinition n) {
        // nothing to do
    }

    @Override
    public void visit(TypeDefinitionLink n) {
        // nothing to do
    }

    @Override
    public void visit(InterfaceDefinition n) {
        // nothing to do
    }

    @Override
    public void visit(DocumentationComment n) {
        // nothing to do
    }

    @Override
    public void visit(FreshValueExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(CourierDefinitionNode n) {
       // TODO
    }

    @Override
    public void visit(CourierChoiceStatement n) {
        // TODO
    }

    @Override
    public void visit(NotificationForwardStatement n) {
        // nothing to do
    }

    @Override
    public void visit(SolicitResponseForwardStatement n) {
        // TODO
    }

    @Override
    public void visit(InterfaceExtenderDefinition n) {
        // nothing to do
    }

    @Override
    public void visit(InlineTreeExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(VoidExpressionNode n) {
        // nothing to do
    }

    @Override
    public void visit(ProvideUntilStatement n) {
        n.until().accept( this );
        n.provide().accept( this );
    }

    @Override
    public void visit(TypeChoiceDefinition n) {
        // nothing to do

    }

    public void validate()
    {
        program.accept( this );

        HashMap<String,OperationDeclaration> inputOperationMap = new HashMap<>();
        for ( Map.Entry<String,InputPortInfo> inputPortInfoEntry : inputPortInfoMap.entrySet() ) {
            for( InterfaceDefinition intf : inputPortInfoEntry.getValue().getInterfaceList() ) {
                for( Map.Entry<String,OperationDeclaration> e : intf.operationsMap().entrySet() ) {
                    inputOperationMap.put(e.getKey(), e.getValue());
                }
            }
        }

        for( Map.Entry<String,ScopeTree> scope : mainScopes.entrySet() ) {
            scope.getValue().checkFaults();

            // check if the faults are declared in the related interface
            if ( inputOperationMap.get( scope.getKey() ) instanceof RequestResponseOperationDeclaration ) {
                RequestResponseOperationDeclaration rr = ( RequestResponseOperationDeclaration )  inputOperationMap.get( scope.getKey() );
                for( String f : rr.faults().keySet() ) {
                    if ( !scope.getValue().getThrownFaultList().containsKey( f ) && !scope.getValue().getThrownFaultList().isEmpty() ) {
                        System.out.println("CODE QUALITY WARNING: fault " + f + " is never thrown by operation " + rr.id() + " even if it is declared in the interface");
                    }
                }
                for( Map.Entry<String,ThrownFault> f : scope.getValue().getThrownFaultList().entrySet() ) {
                    if ( !rr.faults().containsKey( f.getKey() ) ) {
                        if ( f.getValue().raisedBy == null ) {
                            System.out.println("CODE QUALITY WARNING: fault " + f.getKey() + " could be thrown inside operation " + rr.id() + " but it is not declared in the interface");
                        } else {
                            System.out.println("CODE QUALITY WARNING: fault " + f.getKey() + " could be thrown inside operation " + rr.id() + " raised by " + f.getValue().raisedBy + " but it is not declared in the interface");
                        }
                    }
                }
            }

        }


    }

    private OperationDeclaration getOperationDeclarationFromInterface( String outputPortId, String operation ) {
        OutputPortInfo outputPortInfo = outputPortInfoMap.get(outputPortId );

        for( InterfaceDefinition intf : outputPortInfo.getInterfaceList() ) {
            if ( intf.operationsMap().get( operation ) != null ) {
                return intf.operationsMap().get( operation );
            }
        }
        return null;
    }
}

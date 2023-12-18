
package xml

import _root_.play.twirl.api.JavaScript
import _root_.play.twirl.api.Xml
import _root_.play.twirl.api.Html
import _root_.play.twirl.api.TwirlHelperImports._
import _root_.play.twirl.api.TwirlFeatureImports._
import _root_.play.twirl.api.Txt
/*1.2*/import org.intocps.verification.scenarioverifier.core._

object FMI3CosimUppaalTemplate extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.XmlFormat.Appendable,_root_.play.twirl.api.Format[play.twirl.api.XmlFormat.Appendable]](play.twirl.api.XmlFormat) with _root_.play.twirl.api.Template1[ModelEncoding,play.twirl.api.XmlFormat.Appendable] {

  /**/
  def apply/*2.10*/(m: ModelEncoding):play.twirl.api.XmlFormat.Appendable = {
    _display_ {
      {


Seq[Any](format.raw/*3.1*/("""        """),format.raw/*3.9*/("""<?xml version="1.0" encoding="utf-8"?>
        <!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_2.dtd'>
<nta>
    <declaration>
        //***********************************************************************************************************
        //Do not change
        const int END_TIME = 1;
        const int START_TIME = 0;
        int currentConfig := 0;

        //Is the stepFinder active
        bool stepFinderActive := false;

        //We need to run an extra iteration of the stepFinder and loopSolver with all checks activated
        // once the loop has converged
        bool isLoopExtraIteration := false;
        bool isStepExtraIteration := false;

        //These is simply to keep track of which loop is the inner one - in case of nested loops
        bool isLoopNested := false;
        bool isStepNested := false;

        //Active loop - if it is different form -1, we are in the middle of solving an algebraic loop
        int loopActive := -1;

        //Used in Trace Visualization
        int isInit := 0;
        int isSimulation := 0;

        bool checksDisabled = false;


        //In case of a scenario with algebraic loops and step rejection,
        // we need to be able to turn off the precondition checks
        bool shouldChecksBeDisabled()"""),format.raw/*37.38*/("""{"""),format.raw/*37.39*/("""
            """),format.raw/*38.13*/("""//In case a loop is not activated all checks should be
            if(loopActive == -1 &amp;&amp; !stepFinderActive)"""),format.raw/*39.62*/("""{"""),format.raw/*39.63*/("""
                """),format.raw/*40.17*/("""return false;
            """),format.raw/*41.13*/("""}"""),format.raw/*41.14*/("""

            """),format.raw/*43.13*/("""//We are inside a loop is it nested
            if(isLoopNested || isStepNested)"""),format.raw/*44.45*/("""{"""),format.raw/*44.46*/("""
                """),format.raw/*45.17*/("""//Both loops should be on the extraIteration
                return !(isStepExtraIteration &amp;&amp; isLoopExtraIteration);
            """),format.raw/*47.13*/("""}"""),format.raw/*47.14*/("""

            """),format.raw/*49.13*/("""//Not nested - if none of the loops is in the extra iteration we should disable the checks
            if(!isLoopExtraIteration &amp;&amp; !isStepExtraIteration)"""),format.raw/*50.71*/("""{"""),format.raw/*50.72*/("""
                """),format.raw/*51.17*/("""return true;
            """),format.raw/*52.13*/("""}"""),format.raw/*52.14*/("""

            """),format.raw/*54.13*/("""return false;
        """),format.raw/*55.9*/("""}"""),format.raw/*55.10*/("""

        """),format.raw/*57.9*/("""//FMU of a variable
        const int undefined := 0;
        const int defined := 1;
        const int notStable :=-1;

        //FMU of the variable
        typedef struct """),format.raw/*63.24*/("""{"""),format.raw/*63.25*/("""
            """),format.raw/*64.13*/("""int[-1,1] status;
            int time;
        """),format.raw/*66.9*/("""}"""),format.raw/*66.10*/(""" """),format.raw/*66.11*/("""variable;


        //Const assignment types - to future variables or current:
        const int final := 0;
        const int tentative := 1;
        const int noCommitment := -1;

        //***********************************************************************************************************

        //Max number of inputs/outputs any FMU can have - Should be changed
        const int MaxNInputs = """),_display_(/*77.33*/m/*77.34*/.maxNInputs),format.raw/*77.45*/(""";
        const int MaxNOutputs = """),_display_(/*78.34*/m/*78.35*/.maxNOutputs),format.raw/*78.47*/(""";

        //Numbers of FMUs in scenario - Should be changed
        const int nFMU = """),_display_(/*81.27*/m/*81.28*/.nFMUs),format.raw/*81.34*/(""";

        //number of algebraic loops in scenario - Should be changed
        const int nAlgebraicLoopsInInit := """),_display_(/*84.45*/m/*84.46*/.nAlgebraicLoopsInInit),format.raw/*84.68*/(""";
        const int nAlgebraicLoopsInStep := """),_display_(/*85.45*/m/*85.46*/.nAlgebraicLoopsInStep),format.raw/*85.68*/(""";

        //Adaptive co-simulation - numbers of different configurations
        const int nConfig := """),_display_(/*88.31*/m/*88.32*/.nConfigs),format.raw/*88.41*/(""";
        //***********************************************************************************************************
        //Do not change

        const int NActions := 14;

        //The number of actions in our system
        const int N := MaxNInputs &gt; MaxNOutputs? MaxNInputs : MaxNOutputs;

        //The maximum step allowed in system - shouldn't be changed
        const int H_max := """),_display_(/*98.29*/m/*98.30*/.Hmax),format.raw/*98.35*/(""";
        const int H := H_max;

        const int noStep := -1;
        const int noFMU := -1;
        const int noLoop := -1;

        typedef struct """),format.raw/*105.24*/("""{"""),format.raw/*105.25*/("""
            """),format.raw/*106.13*/("""int[-1, nFMU] FMU;
            int[-1,NActions] act;
            int[-1,N] portVariable;
            int[-1,H] step_size;
            int[-1,nFMU] relative_step_size;
            int[-1,1] commitment;
            int[-1, nAlgebraicLoopsInStep] loop;
        """),format.raw/*113.9*/("""}"""),format.raw/*113.10*/(""" """),format.raw/*113.11*/("""Operation;

        typedef struct """),format.raw/*115.24*/("""{"""),format.raw/*115.25*/("""
            """),format.raw/*116.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNInputs] input;
            int[-1, MaxNOutputs] output;
        """),format.raw/*119.9*/("""}"""),format.raw/*119.10*/(""" """),format.raw/*119.11*/("""InternalConnection;

        //Types of input ports
        const int delayed := 0;
        const int reactive := 1;
        const int noPort := -1;

        typedef struct """),format.raw/*126.24*/("""{"""),format.raw/*126.25*/("""
            """),format.raw/*127.13*/("""int[0, nFMU] SrcFMU;
            int[0,MaxNOutputs] output;
            int[0,nFMU] TrgFMU;
            int[0,MaxNInputs] input;
        """),format.raw/*131.9*/("""}"""),format.raw/*131.10*/(""" """),format.raw/*131.11*/("""ExternalConnection;

        typedef struct """),format.raw/*133.24*/("""{"""),format.raw/*133.25*/("""
            """),format.raw/*134.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNOutputs] port;
        """),format.raw/*136.9*/("""}"""),format.raw/*136.10*/(""" """),format.raw/*136.11*/("""FmuOutputPort;


        //The action dictates which action will be executed
        const int noOp := -1;
        const int get := 0;
        const int set := 1;
        const int step := 2;
        const int save := 3;
        const int restore := 4;
        const int setParameter := 5;
        const int instantiate := 6;
        const int enterInitialization := 7;
        const int exitInitialization := 8;
        const int loop := 9;
        const int findStep := 10;
        const int setupExperiment := 11;
        const int unload := 12;
        const int terminate := 13;
        const int freeInstance := 14;

        int[-1,NActions] action;

        //The activeFMU variable dictates which FMU is enabled - which FMU should take an action
        int[-1, nFMU] activeFMU = 0;

        //Channels
        //A channel to invoke an FMU-action from the Interpreter, StepFinder, LoopSolver
        chan fmu[nFMU];

        //A channel for all actions - I can use fewer channels and use the actions to distinguish
        broadcast chan actionPerformed;

        //A channel to start the stepFinder
        chan findStepChan;

        //Channel to start and finish LoopSolver
        chan solveLoop;

        //Channel to start and finish LoopSolver for Init
        chan solveLoopInit;

        //A broadcase channel for an FMU to signal an error
        chan ErrorChan;

        //The fmu-variable that is updated by the action
        int [-1,N] var = 0;
        //The variable that is being get or set (either the current or future time) by the action
        int [-1,1] commitment = 0;
        //The stepsize of the step action
        int [-1, H_max] stepsize = 0;
        int [-1, nFMU] relative_step_size = 0;

        const int end = END_TIME;
        int time = START_TIME;

        //Counters to track the current FMU of the co-simulation
        int fmusUnloaded = 0;

        //***********************************************************************************************************
        //Scenario Dependent - Should be changed!

        //Number of internal connections - both init and normal
        const int nInternal := """),_display_(/*199.33*/m/*199.34*/.nInternal),format.raw/*199.44*/(""";
        const int nInternalInit := """),_display_(/*200.37*/m/*200.38*/.nInternalInit),format.raw/*200.52*/(""";

        //Number of external connections in scenario
        const int nExternal := """),_display_(/*203.33*/m/*203.34*/.nExternal),format.raw/*203.44*/(""";

        //The initial of value of h
        int h := H_max;

        //This array is representing the variables of the stepSize that each FMU can take - H_max is the default value
        int stepVariables[nFMU] = """),format.raw/*209.35*/("""{"""),_display_(/*209.37*/m/*209.38*/.stepVariables),format.raw/*209.52*/("""}"""),format.raw/*209.53*/(""";

        //A generic action to pick the next action
        void unpackOperation(Operation operation)"""),format.raw/*212.50*/("""{"""),format.raw/*212.51*/("""
            """),format.raw/*213.13*/("""//action to be performed
            action := operation.act;
            //fmu to perform the action
            activeFMU := operation.FMU;
            //The variable involved
            var := operation.portVariable;
            //The Stepsize
            stepsize := operation.step_size;
            //The Stepsize
            relative_step_size := operation.relative_step_size;
            //The commitment
            commitment := operation.commitment;
            if(loopActive == noLoop)"""),format.raw/*225.37*/("""{"""),format.raw/*225.38*/("""
                """),format.raw/*226.17*/("""loopActive := operation.loop;
            """),format.raw/*227.13*/("""}"""),format.raw/*227.14*/("""
            """),format.raw/*228.13*/("""if(action == step)"""),format.raw/*228.31*/("""{"""),format.raw/*228.32*/("""
                """),format.raw/*229.17*/("""if (stepsize == noStep) """),format.raw/*229.41*/("""{"""),format.raw/*229.42*/("""
                    """),format.raw/*230.21*/("""// Step is relative to the fmu referred to by relative_step_size
                    stepsize := stepVariables[relative_step_size];
                """),format.raw/*232.17*/("""}"""),format.raw/*232.18*/(""" """),format.raw/*232.19*/("""else if (stepsize == H) """),format.raw/*232.43*/("""{"""),format.raw/*232.44*/("""
                    """),format.raw/*233.21*/("""// Default step
                    stepsize := h;
                """),format.raw/*235.17*/("""}"""),format.raw/*235.18*/(""" """),format.raw/*235.19*/("""else """),format.raw/*235.24*/("""{"""),format.raw/*235.25*/("""
                    """),format.raw/*236.21*/("""// Absolute step size
                    // Nothing to do.
                """),format.raw/*238.17*/("""}"""),format.raw/*238.18*/("""
            """),format.raw/*239.13*/("""}"""),format.raw/*239.14*/("""
            """),format.raw/*240.13*/("""//Update checkStatus
            checksDisabled = shouldChecksBeDisabled();
        """),format.raw/*242.9*/("""}"""),format.raw/*242.10*/("""


        """),format.raw/*245.9*/("""//Encoding of the scenario
        //Each FMU should have a different ID \in [0, nFMU-1]
        """),_display_(/*247.10*/for(fName<- m.fmuNames) yield /*247.33*/ {_display_(Seq[Any](format.raw/*247.35*/("""
        """),format.raw/*248.9*/("""const int """),_display_(/*248.20*/fName),format.raw/*248.25*/(""" """),format.raw/*248.26*/(""":= """),_display_(/*248.30*/m/*248.31*/.fmuId(fName)),format.raw/*248.44*/(""";
        """)))}),format.raw/*249.10*/("""

        """),format.raw/*251.9*/("""//Number of inputs and outputs of each FMU
        """),_display_(/*252.10*/for(fName<- m.fmuNames) yield /*252.33*/ {_display_(Seq[Any](format.raw/*252.35*/("""
        """),format.raw/*253.9*/("""const int """),_display_(/*253.20*/{fName}),format.raw/*253.27*/("""_input := """),_display_(/*253.38*/m/*253.39*/.nInputs(fName)),format.raw/*253.54*/(""";
        const int """),_display_(/*254.20*/{fName}),format.raw/*254.27*/("""_output := """),_display_(/*254.39*/m/*254.40*/.nOutputs(fName)),format.raw/*254.56*/(""";
        """)))}),format.raw/*255.10*/("""

        """),format.raw/*257.9*/("""//Definition of inputs and outputs of each FMU
        """),_display_(/*258.10*/for(fName<- m.fmuNames) yield /*258.33*/ {_display_(Seq[Any](format.raw/*258.35*/("""
        """),format.raw/*259.9*/("""// """),_display_(/*259.13*/fName),format.raw/*259.18*/(""" """),format.raw/*259.19*/("""inputs - """),_display_(/*259.29*/m/*259.30*/.nInputs(fName)),format.raw/*259.45*/("""
        """),_display_(/*260.10*/for(inName<- m.fmuInNames(fName)) yield /*260.43*/ {_display_(Seq[Any](format.raw/*260.45*/("""
        """),format.raw/*261.9*/("""const int """),_display_(/*261.20*/{m.fmuPortName(fName, inName)}),format.raw/*261.50*/(""" """),format.raw/*261.51*/(""":= """),_display_(/*261.55*/m/*261.56*/.fmuInputEncoding(fName)/*261.80*/(inName)),format.raw/*261.88*/(""";
        """)))}),format.raw/*262.10*/("""
        """),format.raw/*263.9*/("""// """),_display_(/*263.13*/fName),format.raw/*263.18*/(""" """),format.raw/*263.19*/("""outputs - """),_display_(/*263.30*/m/*263.31*/.nOutputs(fName)),format.raw/*263.47*/("""
        """),_display_(/*264.10*/for(outName<- m.fmuOutNames(fName)) yield /*264.45*/ {_display_(Seq[Any](format.raw/*264.47*/("""
        """),format.raw/*265.9*/("""const int """),_display_(/*265.20*/{m.fmuPortName(fName, outName)}),format.raw/*265.51*/(""" """),format.raw/*265.52*/(""":= """),_display_(/*265.56*/m/*265.57*/.fmuOutputEncoding(fName)/*265.82*/(outName)),format.raw/*265.91*/(""";
        """)))}),format.raw/*266.10*/("""
        """),format.raw/*267.9*/("""const int """),_display_(/*267.20*/{fName}),format.raw/*267.27*/("""_inputTypes[nConfig][MaxNInputs] := """),format.raw/*267.63*/("""{"""),format.raw/*267.64*/(""" """),_display_(/*267.66*/m/*267.67*/.fmuInputTypes(fName)),format.raw/*267.88*/(""" """),format.raw/*267.89*/("""}"""),format.raw/*267.90*/(""";
        """)))}),format.raw/*268.10*/("""

        """),format.raw/*270.9*/("""//This array is to keep track of the value of each output port - each output port needs two variables (current and future)
        // and each variable is having two values (defined and time)
        variable connectionVariable[nFMU][MaxNOutputs][2] = """),format.raw/*272.61*/("""{"""),format.raw/*272.62*/(""" """),_display_(/*272.64*/m/*272.65*/.connectionVariable),format.raw/*272.84*/(""" """),format.raw/*272.85*/("""}"""),format.raw/*272.86*/(""";

        //Connections - do not longer contain the type of the input - but it is still a 1:1 mapping
        const ExternalConnection external[nConfig][nExternal] = """),format.raw/*275.65*/("""{"""),format.raw/*275.66*/(""" """),_display_(/*275.68*/m/*275.69*/.external),format.raw/*275.78*/(""" """),format.raw/*275.79*/("""}"""),format.raw/*275.80*/(""";

        const InternalConnection feedthroughInStep[nConfig][nInternal] = """),format.raw/*277.74*/("""{"""),format.raw/*277.75*/(""" """),_display_(/*277.77*/m/*277.78*/.feedthroughInStep),format.raw/*277.96*/(""" """),format.raw/*277.97*/("""}"""),format.raw/*277.98*/(""";

        //The initial internal connection could be different from the connection in the simulation and should be represented differently
        const InternalConnection feedthroughInInit[nInternalInit] = """),format.raw/*280.69*/("""{"""),format.raw/*280.70*/(""" """),_display_(/*280.72*/m/*280.73*/.feedthroughInInit),format.raw/*280.91*/(""" """),format.raw/*280.92*/("""}"""),format.raw/*280.93*/(""";

        //The array show if an FMU can reject a step or not - if the FMU can reject a step the value is 1 on the index defined by the fmus
        const bool mayRejectStep[nFMU] = """),format.raw/*283.42*/("""{"""),format.raw/*283.43*/(""" """),_display_(/*283.45*/m/*283.46*/.mayRejectStep),format.raw/*283.60*/(""" """),format.raw/*283.61*/("""}"""),format.raw/*283.62*/(""";


        const int maxStepOperations := """),_display_(/*286.41*/m/*286.42*/.maxStepOperations),format.raw/*286.60*/(""";

        //Numbers of operations in each step
        const int nInstantiationOperations := """),_display_(/*289.48*/m/*289.49*/.nInstantiationOperations),format.raw/*289.74*/(""";
        const int nInitializationOperations := """),_display_(/*290.49*/m/*290.50*/.nInitializationOperations),format.raw/*290.76*/(""";
        const int[0,maxStepOperations] nStepOperations[nConfig] := """),format.raw/*291.68*/("""{"""),_display_(/*291.70*/m/*291.71*/.nStepOperations),format.raw/*291.87*/("""}"""),format.raw/*291.88*/(""";
        const int nTerminationOperations := """),_display_(/*292.46*/m/*292.47*/.nTerminationOperations),format.raw/*292.70*/(""";

        // Numbers for algebraic loop operations in init
        const int maxNAlgebraicLoopOperationsInInit := """),_display_(/*295.57*/m/*295.58*/.maxNAlgebraicLoopOperationsInInit),format.raw/*295.92*/(""";
        const int maxNConvergeOperationsForAlgebraicLoopsInInit := """),_display_(/*296.69*/m/*296.70*/.maxNConvergeOperationsForAlgebraicLoopsInInit),format.raw/*296.116*/(""";

        //Numbers of operations to be performed per algebraic loop in init
        const int[0,maxNConvergeOperationsForAlgebraicLoopsInInit] nConvergencePortsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit] = """),format.raw/*299.133*/("""{"""),_display_(/*299.135*/m/*299.136*/.nConvergencePortsPerAlgebraicLoopInInit),format.raw/*299.176*/("""}"""),format.raw/*299.177*/(""";
        const int[0,maxNAlgebraicLoopOperationsInInit] nOperationsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit] = """),format.raw/*300.115*/("""{"""),_display_(/*300.117*/m/*300.118*/.nOperationsPerAlgebraicLoopInInit),format.raw/*300.152*/("""}"""),format.raw/*300.153*/(""";



        // Number of operations in the step finding loop
        const int maxFindStepOperations := """),_display_(/*305.45*/m/*305.46*/.maxFindStepOperations),format.raw/*305.68*/(""";
        const int maxFindStepRestoreOperations := """),_display_(/*306.52*/m/*306.53*/.maxFindStepRestoreOperations),format.raw/*306.82*/(""";

        const int[0,maxFindStepOperations] nFindStepOperations[nConfig] := """),format.raw/*308.76*/("""{"""),_display_(/*308.78*/m/*308.79*/.nFindStepOperations),format.raw/*308.99*/("""}"""),format.raw/*308.100*/(""";
        const int[0,maxFindStepRestoreOperations] nRestore[nConfig] := """),format.raw/*309.72*/("""{"""),_display_(/*309.74*/m/*309.75*/.nRestore),format.raw/*309.84*/("""}"""),format.raw/*309.85*/(""";

        // Numbers for algebraic loop operations in step
        const int maxNAlgebraicLoopOperationsInStep := """),_display_(/*312.57*/m/*312.58*/.maxNAlgebraicLoopOperationsInStep),format.raw/*312.92*/(""";
        const int maxNRetryOperationsForAlgebraicLoopsInStep := """),_display_(/*313.66*/m/*313.67*/.maxNRetryOperationsForAlgebraicLoopsInStep),format.raw/*313.110*/(""";
        const int maxNConvergeOperationsForAlgebraicLoopsInStep := """),_display_(/*314.69*/m/*314.70*/.maxNConvergeOperationsForAlgebraicLoopsInStep),format.raw/*314.116*/(""";

        //Numbers of operations to be performed per algebraic loop in step
        const int[0,maxNConvergeOperationsForAlgebraicLoopsInStep] nConvergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*317.142*/("""{"""),_display_(/*317.144*/m/*317.145*/.nConvergencePortsPerAlgebraicLoopInStep),format.raw/*317.185*/("""}"""),format.raw/*317.186*/(""";
        const int[0,maxNAlgebraicLoopOperationsInStep] nOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*318.124*/("""{"""),_display_(/*318.126*/m/*318.127*/.nOperationsPerAlgebraicLoopInStep),format.raw/*318.161*/("""}"""),format.raw/*318.162*/(""";
        const int[0,maxNRetryOperationsForAlgebraicLoopsInStep] nRetryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*319.138*/("""{"""),_display_(/*319.140*/m/*319.141*/.nRetryOperationsPerAlgebraicLoopInStep),format.raw/*319.180*/("""}"""),format.raw/*319.181*/(""";

        //These operations define what should be performed in the simulation - it is assumed that the operation first loads the fmus
        const Operation instantiationOperations[nInstantiationOperations] = """),format.raw/*322.77*/("""{"""),format.raw/*322.78*/(""" """),_display_(/*322.80*/m/*322.81*/.instantiationOperations),format.raw/*322.105*/(""" """),format.raw/*322.106*/("""}"""),format.raw/*322.107*/(""";

        const Operation initializationOperations[nInitializationOperations] = """),format.raw/*324.79*/("""{"""),format.raw/*324.80*/(""" """),_display_(/*324.82*/m/*324.83*/.initializationOperations),format.raw/*324.108*/(""" """),format.raw/*324.109*/("""}"""),format.raw/*324.110*/(""";

        const Operation stepOperations[nConfig][maxStepOperations] = """),format.raw/*326.70*/("""{"""),format.raw/*326.71*/(""" """),_display_(/*326.73*/m/*326.74*/.stepOperations),format.raw/*326.89*/(""" """),format.raw/*326.90*/("""}"""),format.raw/*326.91*/(""";

        //These are the operations to be performed in order to find the correct step
        //In these operation there is a difference on the third parameter to doStep:
        // H (A step-value greater than the allowed step (Greater than the number of FMUS)) means that we should look at the variable h
        // A stepSize (0:(nFMU-1)) means that the should look at that index in stepVariables use that as the step
        //This is being done inside - findStepAction

        const Operation findStepIteration[nConfig][maxFindStepOperations] = """),format.raw/*334.77*/("""{"""),format.raw/*334.78*/(""" """),_display_(/*334.80*/m/*334.81*/.findStepLoopOperations),format.raw/*334.104*/(""" """),format.raw/*334.105*/("""}"""),format.raw/*334.106*/(""";
        const Operation StepFix[nConfig][maxFindStepRestoreOperations] = """),format.raw/*335.74*/("""{"""),format.raw/*335.75*/(""" """),_display_(/*335.77*/m/*335.78*/.findStepLoopRestoreOperations),format.raw/*335.108*/(""" """),format.raw/*335.109*/("""}"""),format.raw/*335.110*/(""";

        //Possible multiple loops
        //Loop operations are to solve algebraic loops in the co-simulation scenario
        const Operation operationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNAlgebraicLoopOperationsInStep] = """),format.raw/*339.127*/("""{"""),_display_(/*339.129*/m/*339.130*/.operationsPerAlgebraicLoopInStep),format.raw/*339.163*/(""" """),format.raw/*339.164*/("""}"""),format.raw/*339.165*/(""";
        const Operation operationsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit][maxNAlgebraicLoopOperationsInInit] = """),format.raw/*340.118*/("""{"""),_display_(/*340.120*/m/*340.121*/.operationsPerAlgebraicLoopInInit),format.raw/*340.154*/(""" """),format.raw/*340.155*/("""}"""),format.raw/*340.156*/(""";

        //The converge ports is to mark which variables that needs to be checked in the convergence loop
        //The convention is now to specify the FMU first and the port to denote the variables that should be checked
        const FmuOutputPort convergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNConvergeOperationsForAlgebraicLoopsInStep] = """),format.raw/*344.149*/("""{"""),_display_(/*344.151*/m/*344.152*/.convergencePortsPerAlgebraicLoopInStep),format.raw/*344.191*/(""" """),format.raw/*344.192*/("""}"""),format.raw/*344.193*/(""";
        const FmuOutputPort convergencePortsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit][maxNConvergeOperationsForAlgebraicLoopsInInit] = """),format.raw/*345.140*/("""{"""),_display_(/*345.142*/m/*345.143*/.convergencePortsPerAlgebraicLoopInInit),format.raw/*345.182*/(""" """),format.raw/*345.183*/("""}"""),format.raw/*345.184*/(""";

        const Operation retryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNRetryOperationsForAlgebraicLoopsInStep] = """),format.raw/*347.141*/("""{"""),_display_(/*347.143*/m/*347.144*/.retryOperationsPerAlgebraicLoopInStep),format.raw/*347.182*/(""" """),format.raw/*347.183*/("""}"""),format.raw/*347.184*/(""";

        const Operation terminationOperations[nTerminationOperations] = """),format.raw/*349.73*/("""{"""),format.raw/*349.74*/(""" """),_display_(/*349.76*/m/*349.77*/.terminationOperations),format.raw/*349.99*/(""" """),format.raw/*349.100*/("""}"""),format.raw/*349.101*/(""";

    </declaration>
<template>
<name>Interpreter</name>
<declaration>
    int inst_pc := 0;
    int init_pc := 0;
    int cosimstep_pc := 0;
    int terminate_pc := 0;
    int n := 0;

    void selectNextInstAction()"""),format.raw/*361.32*/("""{"""),format.raw/*361.33*/("""
        """),format.raw/*362.9*/("""unpackOperation(instantiationOperations[inst_pc]);
        //Proceed to next action
        inst_pc++;
    """),format.raw/*365.5*/("""}"""),format.raw/*365.6*/("""

    """),format.raw/*367.5*/("""void selectNextInitAction()"""),format.raw/*367.32*/("""{"""),format.raw/*367.33*/("""
        """),format.raw/*368.9*/("""unpackOperation(initializationOperations[init_pc]);
        //Proceed to next action
        init_pc++;
    """),format.raw/*371.5*/("""}"""),format.raw/*371.6*/("""


    """),format.raw/*374.5*/("""void selectNextCosimStepAction()"""),format.raw/*374.37*/("""{"""),format.raw/*374.38*/("""
        """),format.raw/*375.9*/("""if(cosimstep_pc &lt; nStepOperations[currentConfig])"""),format.raw/*375.61*/("""{"""),format.raw/*375.62*/("""
            """),format.raw/*376.13*/("""unpackOperation(stepOperations[currentConfig][cosimstep_pc]);
        """),format.raw/*377.9*/("""}"""),format.raw/*377.10*/("""
        """),format.raw/*378.9*/("""//Proceed to next action
        cosimstep_pc++;
    """),format.raw/*380.5*/("""}"""),format.raw/*380.6*/("""

    """),format.raw/*382.5*/("""void findFMUTerminateAction()"""),format.raw/*382.34*/("""{"""),format.raw/*382.35*/("""
        """),format.raw/*383.9*/("""unpackOperation(terminationOperations[terminate_pc]);
        //Proceed to next action
        terminate_pc++;
    """),format.raw/*386.5*/("""}"""),format.raw/*386.6*/("""


    """),format.raw/*389.5*/("""void takeStep(int global_h, int newConfig)"""),format.raw/*389.47*/("""{"""),format.raw/*389.48*/("""
        """),format.raw/*390.9*/("""//h is progression of time
        time := time + h;
        //Reset the loop actions
        cosimstep_pc := 0;
        //reset the global stepsize
        h := global_h;
        //reset n
        n := 0;
        currentConfig := newConfig;
    """),format.raw/*399.5*/("""}"""),format.raw/*399.6*/("""

    """),format.raw/*401.5*/("""void setStepsizeFMU(int fmu, int fmu_step_size)"""),format.raw/*401.52*/("""{"""),format.raw/*401.53*/("""
        """),format.raw/*402.9*/("""if(mayRejectStep[fmu])"""),format.raw/*402.31*/("""{"""),format.raw/*402.32*/("""
            """),format.raw/*403.13*/("""//If an FMU can reject a Step it is maximum step should be updated in each iteration
            stepVariables[fmu] = fmu_step_size;
        """),format.raw/*405.9*/("""}"""),format.raw/*405.10*/("""else"""),format.raw/*405.14*/("""{"""),format.raw/*405.15*/("""
            """),format.raw/*406.13*/("""//If not just set its maximum step to the global step
            stepVariables[fmu] = h;
        """),format.raw/*408.9*/("""}"""),format.raw/*408.10*/("""
        """),format.raw/*409.9*/("""n++;
    """),format.raw/*410.5*/("""}"""),format.raw/*410.6*/("""
"""),format.raw/*411.1*/("""</declaration>
<location id="id0" x="569" y="34">
</location>
<location id="id1" x="1113" y="-102">
    <committed/>
</location>
<location id="id2" x="841" y="-102">
</location>
<location id="id3" x="2176" y="-127">
    <name x="2227" y="-152">Error</name>
</location>
<location id="id4" x="2048" y="25">
    <name x="1988" y="68">SolveAlgebraicLoop</name>
</location>
<location id="id5" x="2065" y="-221">
    <name x="2055" y="-255">FindStep</name>
</location>
<location id="id6" x="1351" y="-102">
    <name x="1325" y="-144">CosimStep</name>
    <committed/>
</location>
<location id="id7" x="1521" y="-425">
    <name x="1427" y="-484">NormalFMUAction</name>
</location>
<location id="id8" x="561" y="416">
    <name x="578" y="408">Terminated</name>
</location>
<location id="id9" x="561" y="272">
    <committed/>
</location>
<location id="id10" x="952" y="272">
    <name x="944" y="306">Termination</name>
    <committed/>
</location>
<location id="id11" x="1674" y="-102">
    <name x="1691" y="-68">Simulate</name>
    <committed/>
</location>
<location id="id12" x="569" y="-102">
    <committed/>
</location>
<location id="id13" x="339" y="-102">
    <name x="501" y="-357">Initialization</name>
    <committed/>
</location>
<location id="id14" x="-174" y="-102">
    <name x="-139" y="-340">instantiationOperations</name>
    <committed/>
</location>
<location id="id15" x="107" y="-102">
</location>
<location id="id16" x="-459" y="-102">
    <name x="-469" y="-136">Start</name>
    <committed/>
</location>
<init ref="id16"/>
<transition>
    <source ref="id0"/>
    <target ref="id12"/>
    <label kind="synchronisation" x="654" y="-34">solveLoopInit?</label>
    <nail x="646" y="-34"/>
</transition>
<transition>
    <source ref="id12"/>
    <target ref="id0"/>
    <label kind="guard" x="382" y="-17">action == loop</label>
    <label kind="synchronisation" x="408" y="-59">solveLoopInit!</label>
    <nail x="493" y="-25"/>
</transition>
<transition>
    <source ref="id1"/>
    <target ref="id1"/>
    <label kind="select" x="1020" y="-306">step_fmu:int[1,H_max]</label>
    <label kind="guard" x="1037" y="-289">n &lt; nFMU</label>
    <label kind="assignment" x="1011" y="-272">setStepsizeFMU(n, step_fmu)</label>
    <nail x="1003" y="-246"/>
    <nail x="1198" y="-246"/>
</transition>
<transition>
    <source ref="id1"/>
    <target ref="id6"/>
    <label kind="guard" x="1181" y="-136">n == nFMU</label>
</transition>
<transition>
    <source ref="id11"/>
    <target ref="id1"/>
    <label kind="select" x="1377" y="51">global_h:int[1,H_max], config:int[0,nConfig-1]</label>
    <label kind="guard" x="1079" y="34">cosimstep_pc == nStepOperations[currentConfig] + 1
        &amp;&amp; time &lt; end</label>
    <label kind="assignment" x="1470" y="17">takeStep(global_h,config), isSimulation= 0</label>
    <nail x="1351" y="34"/>
</transition>
<transition>
    <source ref="id2"/>
    <target ref="id1"/>
    <label kind="guard" x="859" y="-136">init_pc == nInitializationOperations</label>
    <label kind="synchronisation" x="859" y="-119">actionPerformed?</label>
    <label kind="assignment" x="858" y="-85">isInit = 0</label>
</transition>
<transition>
    <source ref="id2"/>
    <target ref="id12"/>
    <label kind="guard" x="586" y="-314">init_pc &lt; nInitializationOperations</label>
    <label kind="synchronisation" x="587" y="-297">actionPerformed?</label>
    <label kind="assignment" x="586" y="-280">selectNextInitAction()</label>
    <nail x="841" y="-280"/>
    <nail x="569" y="-280"/>
</transition>
<transition>
    <source ref="id12"/>
    <target ref="id2"/>
    <label kind="guard" x="603" y="-204">action == get ||
        action == set ||
        action == exitInitialization ||
        action == enterInitialization</label>
    <label kind="synchronisation" x="587" y="-123">fmu[activeFMU]!</label>
</transition>
<transition>
    <source ref="id13"/>
    <target ref="id12"/>
    <label kind="assignment" x="357" y="-102">selectNextInitAction(), isInit = 1</label>
</transition>
<transition>
    <source ref="id15"/>
    <target ref="id13"/>
    <label kind="guard" x="127" y="-136">inst_pc == nInstantiationOperations</label>
    <label kind="synchronisation" x="135" y="-119">actionPerformed?</label>
</transition>
<transition>
    <source ref="id4"/>
    <target ref="id3"/>
    <label kind="synchronisation" x="1980" y="-17">ErrorChan?</label>
    <nail x="2176" y="25"/>
</transition>
<transition>
    <source ref="id5"/>
    <target ref="id3"/>
    <label kind="synchronisation" x="2065" y="-255">ErrorChan?</label>
    <nail x="2176" y="-221"/>
</transition>
<transition>
    <source ref="id4"/>
    <target ref="id11"/>
    <label kind="synchronisation" x="1784" y="-26">solveLoop?</label>
    <label kind="assignment" x="1801" y="8">selectNextCosimStepAction()</label>
</transition>
<transition>
    <source ref="id11"/>
    <target ref="id4"/>
    <label kind="guard" x="1903" y="-93">loopActive != -1
        &amp;&amp; action == loop</label>
    <label kind="synchronisation" x="1818" y="-127">solveLoop!</label>
    <nail x="2065" y="-110"/>
</transition>
<transition>
    <source ref="id5"/>
    <target ref="id11"/>
    <label kind="synchronisation" x="1861" y="-416">findStepChan?</label>
    <label kind="assignment" x="1861" y="-391">selectNextCosimStepAction(),
        stepFinderActive := false</label>
    <nail x="1946" y="-365"/>
</transition>
<transition>
    <source ref="id11"/>
    <target ref="id5"/>
    <label kind="guard" x="1844" y="-221">action == findStep</label>
    <label kind="synchronisation" x="2005" y="-170">findStepChan!</label>
    <label kind="assignment" x="1997" y="-195">stepFinderActive := true</label>
</transition>
<transition>
    <source ref="id6"/>
    <target ref="id11"/>
    <label kind="assignment" x="1428" y="-144">selectNextCosimStepAction(), isSimulation = 1</label>
    <nail x="1521" y="-102"/>
</transition>
<transition>
    <source ref="id8"/>
    <target ref="id8"/>
    <nail x="442" y="476"/>
    <nail x="442" y="391"/>
</transition>
<transition>
    <source ref="id7"/>
    <target ref="id11"/>
    <label kind="synchronisation" x="1631" y="-493">actionPerformed?</label>
    <label kind="assignment" x="1631" y="-467">selectNextCosimStepAction()</label>
    <nail x="1716" y="-442"/>
</transition>
<transition>
    <source ref="id11"/>
    <target ref="id7"/>
    <label kind="guard" x="1334" y="-382">(action == get ||
        action == set ||
        action == step ||
        action == save ||
        action == restore)
        &amp;&amp; cosimstep_pc &lt; (nStepOperations[currentConfig] +1)</label>
    <label kind="synchronisation" x="1470" y="-255">fmu[activeFMU]!</label>
</transition>
<transition>
    <source ref="id9"/>
    <target ref="id10"/>
    <label kind="guard" x="578" y="120">terminate_pc &lt; nTerminationOperations</label>
    <label kind="assignment" x="680" y="86">findFMUTerminateAction()</label>
    <nail x="561" y="153"/>
    <nail x="952" y="153"/>
</transition>
<transition>
    <source ref="id9"/>
    <target ref="id8"/>
    <label kind="guard" x="570" y="332">terminate_pc == nTerminationOperations</label>
</transition>
<transition>
    <source ref="id10"/>
    <target ref="id9"/>
    <label kind="guard" x="655" y="187">action == unload ||
        action == freeInstance ||
        action == terminate</label>
    <label kind="synchronisation" x="697" y="247">fmu[activeFMU]!</label>
</transition>
<transition>
    <source ref="id11"/>
    <target ref="id10"/>
    <label kind="guard" x="1436" y="102">cosimstep_pc == nStepOperations[currentConfig] + 1
        &amp;&amp; time &gt;= end</label>
    <label kind="assignment" x="1436" y="161">findFMUTerminateAction(), isSimulation = 0</label>
    <nail x="1683" y="272"/>
</transition>
<transition>
    <source ref="id14"/>
    <target ref="id15"/>
    <label kind="guard" x="-120" y="-93">action == instantiate ||
        action == setParameter ||
        action == setupExperiment</label>
    <label kind="synchronisation" x="-106" y="-127">fmu[activeFMU]!</label>
</transition>
<transition>
    <source ref="id15"/>
    <target ref="id14"/>
    <label kind="guard" x="-156" y="-297">inst_pc &lt; nInstantiationOperations</label>
    <label kind="synchronisation" x="-156" y="-280">actionPerformed?</label>
    <label kind="assignment" x="-127" y="-263">selectNextInstAction()</label>
    <nail x="107" y="-263"/>
    <nail x="-174" y="-263"/>
</transition>
<transition>
    <source ref="id16"/>
    <target ref="id14"/>
    <label kind="assignment" x="-383" y="-102">selectNextInstAction()</label>
</transition>
</template>
<template>
<name>LoopSolverInit</name>
<parameter>const int maxIteration</parameter>
<declaration>
    int convergence_pc := 0;

    //Number of iteration run in the loop Solver
    int currentIteration := 0;

    //for index
    int i := 0;

    void selectNextLoopAction(int l)"""),format.raw/*674.37*/("""{"""),format.raw/*674.38*/("""
        """),format.raw/*675.9*/("""unpackOperation(operationsPerAlgebraicLoopInInit[l][convergence_pc]);
        //Proceed to next action
        convergence_pc ++;
    """),format.raw/*678.5*/("""}"""),format.raw/*678.6*/("""

    """),format.raw/*680.5*/("""void updateConvergenceVariables(int l)"""),format.raw/*680.43*/("""{"""),format.raw/*680.44*/("""
        """),format.raw/*681.9*/("""int fmu;
        int v;
        for(i = 0; i &lt; nConvergencePortsPerAlgebraicLoopInInit[l]; i++)"""),format.raw/*683.75*/("""{"""),format.raw/*683.76*/("""
            """),format.raw/*684.13*/("""fmu = convergencePortsPerAlgebraicLoopInInit[l][i].FMU;
            v = convergencePortsPerAlgebraicLoopInInit[l][i].port;
            connectionVariable[fmu][v][tentative].status = connectionVariable[fmu][v][final].status;
            connectionVariable[fmu][v][tentative].time = connectionVariable[fmu][v][final].time;
        """),format.raw/*688.9*/("""}"""),format.raw/*688.10*/("""
    """),format.raw/*689.5*/("""}"""),format.raw/*689.6*/("""

    """),format.raw/*691.5*/("""void loopConverge()"""),format.raw/*691.24*/("""{"""),format.raw/*691.25*/("""
        """),format.raw/*692.9*/("""//Loop not longer active
        loopActive := -1;
        //Loop action counter reset
        convergence_pc := 0;
        //Reset convergence counter
        currentIteration := 0;
    """),format.raw/*698.5*/("""}"""),format.raw/*698.6*/("""


    """),format.raw/*701.5*/("""void resetConvergenceloop()"""),format.raw/*701.32*/("""{"""),format.raw/*701.33*/("""
        """),format.raw/*702.9*/("""convergence_pc := 0;
        selectNextLoopAction(loopActive);
    """),format.raw/*704.5*/("""}"""),format.raw/*704.6*/("""

    """),format.raw/*706.5*/("""//Convergence will happen when all convergenceVariables have a similar future and current value
    bool convergenceCriteria(int l)"""),format.raw/*707.36*/("""{"""),format.raw/*707.37*/("""
        """),format.raw/*708.9*/("""return forall(x:int[0,maxNConvergeOperationsForAlgebraicLoopsInInit-1])
            convergencePortsPerAlgebraicLoopInInit[l][x].FMU != noFMU imply connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][final].status
            == connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][tentative].status
            &amp;&amp;
            connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][final].time
            == connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][tentative].time;
    """),format.raw/*714.5*/("""}"""),format.raw/*714.6*/("""

    """),format.raw/*716.5*/("""bool convergence(int l)"""),format.raw/*716.28*/("""{"""),format.raw/*716.29*/("""
        """),format.raw/*717.9*/("""return (convergenceCriteria(l) &amp;&amp; isLoopExtraIteration);
    """),format.raw/*718.5*/("""}"""),format.raw/*718.6*/("""


    """),format.raw/*721.5*/("""void updateIsExtra(int l)"""),format.raw/*721.30*/("""{"""),format.raw/*721.31*/("""
        """),format.raw/*722.9*/("""if(convergenceCriteria(l))"""),format.raw/*722.35*/("""{"""),format.raw/*722.36*/("""
            """),format.raw/*723.13*/("""isLoopExtraIteration := true;
        """),format.raw/*724.9*/("""}"""),format.raw/*724.10*/("""
    """),format.raw/*725.5*/("""}"""),format.raw/*725.6*/("""
"""),format.raw/*726.1*/("""</declaration>
<location id="id17" x="-1011" y="-518">
    <committed/>
</location>
<location id="id18" x="-94" y="-816">
    <name x="-146" y="-850">NotConverging</name>
</location>
<location id="id19" x="-391" y="-247">
    <name x="-340" y="-213">UpdateVariables</name>
    <committed/>
</location>
<location id="id20" x="-391" y="-510">
    <name x="-357" y="-519">CheckConvergence</name>
    <committed/>
</location>
<location id="id21" x="-1173" y="-416">
</location>
<location id="id22" x="-1343" y="-518">
    <committed/>
</location>
<location id="id23" x="-1708" y="-518">
</location>
<init ref="id23"/>
<transition>
    <source ref="id20"/>
    <target ref="id19"/>
    <label kind="guard" x="-391" y="-417">!convergence(loopActive) &amp;&amp;
        currentIteration &lt; maxIteration</label>
    <label kind="assignment" x="-374" y="-349">updateIsExtra(loopActive)</label>
</transition>
<transition>
    <source ref="id17"/>
    <target ref="id22"/>
    <label kind="guard" x="-1368" y="-671">convergence_pc &lt; nOperationsPerAlgebraicLoopInInit[loopActive]</label>
    <label kind="assignment" x="-1283" y="-510">selectNextLoopAction(loopActive)</label>
    <nail x="-1173" y="-620"/>
</transition>
<transition>
    <source ref="id17"/>
    <target ref="id20"/>
    <label kind="guard" x="-884" y="-569">convergence_pc == nOperationsPerAlgebraicLoopInInit[loopActive]</label>
    <label kind="assignment" x="-993" y="-518">currentIteration++</label>
</transition>
<transition>
    <source ref="id21"/>
    <target ref="id17"/>
    <label kind="synchronisation" x="-1020" y="-433">actionPerformed?</label>
</transition>
<transition>
    <source ref="id22"/>
    <target ref="id21"/>
    <label kind="guard" x="-1249" y="-374">action == get ||
        action == set</label>
    <label kind="synchronisation" x="-1282" y="-403">fmu[activeFMU]!</label>
</transition>
<transition>
    <source ref="id19"/>
    <target ref="id22"/>
    <label kind="assignment" x="-1402" y="-212">updateConvergenceVariables(loopActive),
        resetConvergenceloop()</label>
    <nail x="-1309" y="-246"/>
    <nail x="-1343" y="-246"/>
</transition>
<transition>
    <source ref="id20"/>
    <target ref="id18"/>
    <label kind="guard" x="-468" y="-859">!convergence(loopActive) &amp;&amp;
        currentIteration == maxIteration</label>
    <label kind="synchronisation" x="-298" y="-884">ErrorChan!</label>
    <nail x="-323" y="-620"/>
    <nail x="-323" y="-816"/>
</transition>
<transition>
    <source ref="id20"/>
    <target ref="id23"/>
    <label kind="guard" x="-1249" y="-816">convergence(loopActive)</label>
    <label kind="synchronisation" x="-1071" y="-816">solveLoopInit!</label>
    <label kind="assignment" x="-1385" y="-859">loopConverge(),
        isLoopExtraIteration:= false</label>
    <nail x="-391" y="-782"/>
    <nail x="-1105" y="-790"/>
    <nail x="-1708" y="-790"/>
</transition>
<transition>
    <source ref="id23"/>
    <target ref="id22"/>
    <label kind="guard" x="-1581" y="-569">loopActive != -1
        &amp;&amp; action == loop</label>
    <label kind="synchronisation" x="-1505" y="-536">solveLoopInit?</label>
    <label kind="assignment" x="-1632" y="-493">selectNextLoopAction(loopActive),
        currentIteration := 0</label>
    <nail x="-1377" y="-518"/>
</transition>
</template>
<template>
<name>LoopSolver</name>
<parameter>int maxIteration</parameter>
<declaration>
    int convergence_pc := 0;
    int restore_pc := 0;
    bool isFeedthrough = false;

    //Number of iteration run in the loop Solver
    int currentConvergeLoopIteration := 0;



    void selectNextLoopAction(int l)"""),format.raw/*833.37*/("""{"""),format.raw/*833.38*/("""
        """),format.raw/*834.9*/("""unpackOperation(operationsPerAlgebraicLoopInStep[currentConfig][l][convergence_pc]);
        //Proceed to next action
        convergence_pc ++;
    """),format.raw/*837.5*/("""}"""),format.raw/*837.6*/("""

    """),format.raw/*839.5*/("""void selectNextRestoreAction(int l)"""),format.raw/*839.40*/("""{"""),format.raw/*839.41*/("""
        """),format.raw/*840.9*/("""unpackOperation(retryOperationsPerAlgebraicLoopInStep[currentConfig][l][restore_pc]);
        restore_pc++;
    """),format.raw/*842.5*/("""}"""),format.raw/*842.6*/("""


    """),format.raw/*845.5*/("""void updateConvergenceVariables(int l)"""),format.raw/*845.43*/("""{"""),format.raw/*845.44*/("""
        """),format.raw/*846.9*/("""int fmu;
        int v;
        int i = 0;
        for(i = 0; i &lt; nConvergencePortsPerAlgebraicLoopInStep[currentConfig][l]; i++)"""),format.raw/*849.90*/("""{"""),format.raw/*849.91*/("""
            """),format.raw/*850.13*/("""fmu = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].FMU;
            v = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].port;
            if(isFeedthrough)"""),format.raw/*852.30*/("""{"""),format.raw/*852.31*/("""
                """),format.raw/*853.17*/("""connectionVariable[fmu][v][tentative].status := connectionVariable[fmu][v][final].status;
                connectionVariable[fmu][v][tentative].time := connectionVariable[fmu][v][final].time;
            """),format.raw/*855.13*/("""}"""),format.raw/*855.14*/("""else"""),format.raw/*855.18*/("""{"""),format.raw/*855.19*/("""
                """),format.raw/*856.17*/("""connectionVariable[fmu][v][final].status := connectionVariable[fmu][v][tentative].status;
                connectionVariable[fmu][v][final].time := connectionVariable[fmu][v][tentative].time;
            """),format.raw/*858.13*/("""}"""),format.raw/*858.14*/("""
        """),format.raw/*859.9*/("""}"""),format.raw/*859.10*/("""
    """),format.raw/*860.5*/("""}"""),format.raw/*860.6*/("""

    """),format.raw/*862.5*/("""void loopConverge()"""),format.raw/*862.24*/("""{"""),format.raw/*862.25*/("""
        """),format.raw/*863.9*/("""//Loop not longer active
        loopActive := -1;
        //Loop action counter reset
        convergence_pc := 0;
        //Reset convergence counter
        currentConvergeLoopIteration := 0;
        isLoopExtraIteration:= false;
        isFeedthrough := false;
    """),format.raw/*871.5*/("""}"""),format.raw/*871.6*/("""


    """),format.raw/*874.5*/("""void resetConvergenceloop()"""),format.raw/*874.32*/("""{"""),format.raw/*874.33*/("""
        """),format.raw/*875.9*/("""convergence_pc := 0;
        restore_pc := 0;
        selectNextLoopAction(loopActive);
    """),format.raw/*878.5*/("""}"""),format.raw/*878.6*/("""

    """),format.raw/*880.5*/("""//Convergence will happen when all convergenceVariables have a similar future and current value
    bool convergenceCriteria(int l)"""),format.raw/*881.36*/("""{"""),format.raw/*881.37*/("""
        """),format.raw/*882.9*/("""return forall(x:int[0,maxNConvergeOperationsForAlgebraicLoopsInStep-1])
            convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU != noFMU imply
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].status
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].status
            &amp;&amp;
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].time
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].time;
    """),format.raw/*891.5*/("""}"""),format.raw/*891.6*/("""

    """),format.raw/*893.5*/("""bool convergence(int l)"""),format.raw/*893.28*/("""{"""),format.raw/*893.29*/("""
        """),format.raw/*894.9*/("""return (convergenceCriteria(l) &amp;&amp; isLoopExtraIteration);
    """),format.raw/*895.5*/("""}"""),format.raw/*895.6*/("""


    """),format.raw/*898.5*/("""void updateIsExtra(int l)"""),format.raw/*898.30*/("""{"""),format.raw/*898.31*/("""
        """),format.raw/*899.9*/("""if(convergenceCriteria(l))"""),format.raw/*899.35*/("""{"""),format.raw/*899.36*/("""
            """),format.raw/*900.13*/("""isLoopExtraIteration := true;
        """),format.raw/*901.9*/("""}"""),format.raw/*901.10*/("""
    """),format.raw/*902.5*/("""}"""),format.raw/*902.6*/("""
"""),format.raw/*903.1*/("""</declaration>
<location id="id24" x="-1011" y="-518">
    <committed/>
</location>
<location id="id25" x="-1079" y="-663">
</location>
<location id="id26" x="-442" y="-824">
    <name x="-494" y="-858">NotConverging</name>
</location>
<location id="id27" x="-340" y="-255">
    <name x="-297" y="-229">UpdateVariables</name>
    <committed/>
</location>
<location id="id28" x="-17" y="-518">
</location>
<location id="id29" x="-340" y="-518">
    <name x="-332" y="-509">RestoreState</name>
    <committed/>
</location>
<location id="id30" x="-739" y="-518">
    <name x="-714" y="-543">CheckConvergence</name>
    <committed/>
</location>
<location id="id31" x="-1088" y="-357">
</location>
<location id="id32" x="-1343" y="-518">
    <committed/>
</location>
<location id="id33" x="-1708" y="-518">
</location>
<init ref="id33"/>
<transition>
    <source ref="id29"/>
    <target ref="id27"/>
    <label kind="guard" x="-476" y="-382">action == noOp</label>
    <label kind="assignment" x="-493" y="-357">isFeedthrough = true</label>
</transition>
<transition>
    <source ref="id24"/>
    <target ref="id32"/>
    <label kind="guard" x="-1283" y="-748">convergence_pc &lt; nOperationsPerAlgebraicLoopInStep[currentConfig][loopActive]</label>
    <label kind="assignment" x="-1283" y="-510">selectNextLoopAction(loopActive)</label>
</transition>
<transition>
    <source ref="id32"/>
    <target ref="id25"/>
    <label kind="guard" x="-1292" y="-646">action == findStep</label>
    <label kind="synchronisation" x="-1317" y="-612">findStepChan!</label>
    <label kind="assignment" x="-1334" y="-680">isStepNested := true</label>
</transition>
<transition>
    <source ref="id24"/>
    <target ref="id30"/>
    <label kind="guard" x="-1003" y="-467">convergence_pc == nOperationsPerAlgebraicLoopInStep[currentConfig][loopActive]</label>
    <label kind="assignment" x="-993" y="-518">currentConvergeLoopIteration++</label>
</transition>
<transition>
    <source ref="id25"/>
    <target ref="id24"/>
    <label kind="synchronisation" x="-1011" y="-612">findStepChan?</label>
    <label kind="assignment" x="-1020" y="-586">isStepNested = false</label>
</transition>
<transition>
    <source ref="id31"/>
    <target ref="id24"/>
    <label kind="synchronisation" x="-1020" y="-433">actionPerformed?</label>
</transition>
<transition>
    <source ref="id32"/>
    <target ref="id31"/>
    <label kind="guard" x="-1309" y="-374">action == get ||
        action == set ||
        action == step</label>
    <label kind="synchronisation" x="-1282" y="-403">fmu[activeFMU]!</label>
</transition>
<transition>
    <source ref="id27"/>
    <target ref="id32"/>
    <label kind="assignment" x="-1402" y="-212">updateConvergenceVariables(loopActive),
        resetConvergenceloop()</label>
    <nail x="-1309" y="-246"/>
    <nail x="-1343" y="-246"/>
</transition>
<transition>
    <source ref="id30"/>
    <target ref="id26"/>
    <label kind="guard" x="-816" y="-867">!convergence(loopActive) &amp;&amp;
        currentConvergeLoopIteration == maxIteration</label>
    <label kind="synchronisation" x="-646" y="-892">ErrorChan!</label>
    <nail x="-671" y="-628"/>
    <nail x="-671" y="-824"/>
</transition>
<transition>
    <source ref="id28"/>
    <target ref="id27"/>
    <label kind="guard" x="-195" y="-416">restore_pc == nRetryOperationsPerAlgebraicLoopInStep[currentConfig][loopActive]</label>
    <label kind="synchronisation" x="-221" y="-280">actionPerformed?</label>
    <nail x="-17" y="-255"/>
</transition>
<transition>
    <source ref="id28"/>
    <target ref="id29"/>
    <label kind="guard" x="-272" y="-731">restore_pc &lt; nRetryOperationsPerAlgebraicLoopInStep[currentConfig][loopActive]</label>
    <label kind="synchronisation" x="-272" y="-705">actionPerformed?</label>
    <label kind="assignment" x="-297" y="-663">selectNextRestoreAction(loopActive)</label>
    <nail x="-17" y="-680"/>
    <nail x="-340" y="-680"/>
</transition>
<transition>
    <source ref="id29"/>
    <target ref="id28"/>
    <label kind="guard" x="-255" y="-586">action == restore</label>
    <label kind="synchronisation" x="-247" y="-552">fmu[activeFMU]!</label>
    <nail x="-213" y="-518"/>
</transition>
<transition>
    <source ref="id30"/>
    <target ref="id33"/>
    <label kind="guard" x="-1249" y="-816">convergence(loopActive)</label>
    <label kind="synchronisation" x="-1071" y="-816">solveLoop!</label>
    <label kind="assignment" x="-1385" y="-859">loopConverge()</label>
    <nail x="-739" y="-790"/>
    <nail x="-1105" y="-790"/>
    <nail x="-1708" y="-790"/>
</transition>
<transition>
    <source ref="id30"/>
    <target ref="id29"/>
    <label kind="guard" x="-671" y="-586">!convergence(loopActive) &amp;&amp;
        currentConvergeLoopIteration &lt; maxIteration</label>
    <label kind="assignment" x="-663" y="-510">selectNextRestoreAction(loopActive),
        updateIsExtra(loopActive)</label>
</transition>
<transition>
    <source ref="id33"/>
    <target ref="id32"/>
    <label kind="guard" x="-1581" y="-569">loopActive != -1
        &amp;&amp; action == loop</label>
    <label kind="synchronisation" x="-1505" y="-536">solveLoop?</label>
    <label kind="assignment" x="-1632" y="-493">selectNextLoopAction(loopActive),
        currentConvergeLoopIteration := 0</label>
    <nail x="-1377" y="-518"/>
</transition>
</template>
<template>
<name>StepFinder</name>
<parameter>const int maxTries</parameter>
<declaration>
    int step_pc := 0;
    int restore_pc := 0;
    int numbersOfTries := 0;

    void selectNextStepFinderAction()"""),format.raw/*1055.38*/("""{"""),format.raw/*1055.39*/("""
        """),format.raw/*1056.9*/("""unpackOperation(findStepIteration[currentConfig][step_pc]);
        step_pc++;
    """),format.raw/*1058.5*/("""}"""),format.raw/*1058.6*/("""

    """),format.raw/*1060.5*/("""void selectNextStepRestoreAction()"""),format.raw/*1060.39*/("""{"""),format.raw/*1060.40*/("""
        """),format.raw/*1061.9*/("""unpackOperation(StepFix[currentConfig][restore_pc]);
        restore_pc++;
    """),format.raw/*1063.5*/("""}"""),format.raw/*1063.6*/("""

    """),format.raw/*1065.5*/("""void findMinStep()"""),format.raw/*1065.23*/("""{"""),format.raw/*1065.24*/("""
        """),format.raw/*1066.9*/("""//Maximum step size allowed
        int min = nFMU;
        int j := 0;
        for(j = 0; j &lt; nFMU; j++)"""),format.raw/*1069.37*/("""{"""),format.raw/*1069.38*/("""
            """),format.raw/*1070.13*/("""if(stepVariables[j] &lt; min)"""),format.raw/*1070.42*/("""{"""),format.raw/*1070.43*/("""
                """),format.raw/*1071.17*/("""min := stepVariables[j];
            """),format.raw/*1072.13*/("""}"""),format.raw/*1072.14*/("""
        """),format.raw/*1073.9*/("""}"""),format.raw/*1073.10*/("""
        """),format.raw/*1074.9*/("""h := min;
    """),format.raw/*1075.5*/("""}"""),format.raw/*1075.6*/("""


    """),format.raw/*1078.5*/("""bool stepFound()"""),format.raw/*1078.21*/("""{"""),format.raw/*1078.22*/("""
        """),format.raw/*1079.9*/("""//All FMU that may reject a step should be able to take the same step - h
        return forall(x:int[0, nFMU-1]) mayRejectStep[x] imply stepVariables[x] == h;
    """),format.raw/*1081.5*/("""}"""),format.raw/*1081.6*/("""

    """),format.raw/*1083.5*/("""bool loopConverged()"""),format.raw/*1083.25*/("""{"""),format.raw/*1083.26*/("""
        """),format.raw/*1084.9*/("""return (stepFound() &amp;&amp; isStepExtraIteration);
    """),format.raw/*1085.5*/("""}"""),format.raw/*1085.6*/("""


    """),format.raw/*1088.5*/("""void updateIsExtra()"""),format.raw/*1088.25*/("""{"""),format.raw/*1088.26*/("""
        """),format.raw/*1089.9*/("""if(stepFound())"""),format.raw/*1089.24*/("""{"""),format.raw/*1089.25*/("""
            """),format.raw/*1090.13*/("""isStepExtraIteration := true;
            //Reset numbers of tries to 0 - This is to avoid problems with the maximum number of tries and not to active the nested checks
            numbersOfTries := 0;
        """),format.raw/*1093.9*/("""}"""),format.raw/*1093.10*/("""
    """),format.raw/*1094.5*/("""}"""),format.raw/*1094.6*/("""
"""),format.raw/*1095.1*/("""</declaration>
<location id="id34" x="1122" y="-178">
    <committed/>
</location>
<location id="id35" x="-34" y="-178">
    <committed/>
</location>
<location id="id36" x="-144" y="17">
</location>
<location id="id37" x="817" y="144">
    <committed/>
</location>
<location id="id38" x="654" y="-578">
    <name x="644" y="-612">NoCommonStep</name>
</location>
<location id="id39" x="-1427" y="-178">
    <name x="-1478" y="-187">Start</name>
</location>
<location id="id40" x="1011" y="-331">
</location>
<location id="id41" x="732" y="-178">
    <name x="681" y="-237">Reset</name>
    <committed/>
</location>
<location id="id42" x="348" y="-178">
    <committed/>
</location>
<location id="id43" x="-144" y="-382">
</location>
<location id="id44" x="-510" y="-178">
    <committed/>
</location>
<init ref="id39"/>
<transition>
    <source ref="id39"/>
    <target ref="id44"/>
    <label kind="synchronisation" x="-1105" y="-212">findStepChan?</label>
    <label kind="assignment" x="-1156" y="-161">selectNextStepFinderAction()</label>
</transition>
<transition>
    <source ref="id34"/>
    <target ref="id37"/>
    <label kind="guard" x="884" y="110">nRestore[currentConfig] == restore_pc</label>
    <nail x="1147" y="-178"/>
    <nail x="1147" y="144"/>
</transition>
<transition>
    <source ref="id34"/>
    <target ref="id41"/>
    <label kind="guard" x="892" y="-212">restore_pc &lt; nRestore[currentConfig]</label>
    <label kind="assignment" x="901" y="-170">selectNextStepRestoreAction()</label>
</transition>
<transition>
    <source ref="id40"/>
    <target ref="id34"/>
    <label kind="synchronisation" x="1062" y="-289">actionPerformed?</label>
</transition>
<transition>
    <source ref="id35"/>
    <target ref="id44"/>
    <label kind="guard" x="-365" y="-212">step_pc &lt; nFindStepOperations[currentConfig]</label>
    <label kind="assignment" x="-416" y="-178">selectNextStepFinderAction()</label>
</transition>
<transition>
    <source ref="id36"/>
    <target ref="id35"/>
    <label kind="synchronisation" x="-51" y="-85">solveLoop?</label>
    <label kind="assignment" x="-102" y="-51">isLoopNested := false</label>
</transition>
<transition>
    <source ref="id35"/>
    <target ref="id42"/>
    <label kind="guard" x="-16" y="-212">nFindStepOperations[currentConfig] == step_pc</label>
    <label kind="assignment" x="110" y="-161">findMinStep(),
        numbersOfTries++</label>
</transition>
<transition>
    <source ref="id43"/>
    <target ref="id35"/>
    <label kind="synchronisation" x="-85" y="-323">actionPerformed?</label>
</transition>
<transition>
    <source ref="id44"/>
    <target ref="id36"/>
    <label kind="guard" x="-459" y="-51">action == loop</label>
    <label kind="synchronisation" x="-459" y="-76">solveLoop!</label>
    <label kind="assignment" x="-442" y="8">isLoopNested := true</label>
</transition>
<transition>
    <source ref="id37"/>
    <target ref="id44"/>
    <label kind="assignment" x="-459" y="170">step_pc := 0, restore_pc := 0, selectNextStepFinderAction()</label>
    <nail x="-510" y="153"/>
</transition>
<transition>
    <source ref="id42"/>
    <target ref="id38"/>
    <label kind="guard" x="407" y="-561">!stepFound() &amp;&amp;
        numbersOfTries == maxTries</label>
    <label kind="synchronisation" x="484" y="-510">ErrorChan!</label>
</transition>
<transition>
    <source ref="id42"/>
    <target ref="id39"/>
    <label kind="guard" x="-1258" y="-748">loopConverged()</label>
    <label kind="synchronisation" x="-1258" y="-722">findStepChan!</label>
    <label kind="assignment" x="-1258" y="-688">step_pc := 0, isStepExtraIteration := false, restore_pc:=0,
        numbersOfTries := 0</label>
    <nail x="76" y="-612"/>
    <nail x="-1428" y="-612"/>
</transition>
<transition>
    <source ref="id41"/>
    <target ref="id40"/>
    <label kind="synchronisation" x="782" y="-348">fmu[activeFMU]!</label>
</transition>
<transition>
    <source ref="id42"/>
    <target ref="id41"/>
    <label kind="guard" x="416" y="-221">!loopConverged() &amp;&amp;
        numbersOfTries &lt; maxTries</label>
    <label kind="assignment" x="433" y="-161">selectNextStepRestoreAction(),
        updateIsExtra()</label>
</transition>
<transition>
    <source ref="id44"/>
    <target ref="id43"/>
    <label kind="guard" x="-467" y="-382">action == get ||
        action == set ||
        action == step</label>
    <label kind="synchronisation" x="-484" y="-289">fmu[activeFMU]!</label>
</transition>
</template>
<template>
<name>FMU</name>
<parameter>const int id, const int nOutput, const int nInput, const int inputType[nConfig][MaxNInputs]</parameter>
<declaration>
    int cTime := START_TIME;
    variable inputVariables[MaxNInputs] = """),format.raw/*1233.43*/("""{"""),format.raw/*1233.44*/(""" """),_display_(/*1233.46*/{m.variableArray(m.maxNInputs)}),format.raw/*1233.77*/(""" """),format.raw/*1233.78*/("""}"""),format.raw/*1233.79*/(""";
    variable outputVariables[MaxNOutputs] = """),format.raw/*1234.45*/("""{"""),format.raw/*1234.46*/(""" """),_display_(/*1234.48*/{m.variableArray(m.maxNOutputs)}),format.raw/*1234.80*/(""" """),format.raw/*1234.81*/("""}"""),format.raw/*1234.82*/(""";

    //Index for the for-loop
    int i := 0;

    //Backup FMU
    variable savedOutputVariables[MaxNOutputs];
    variable savedInputVariables[MaxNInputs];
    int savedTime;
    bool isSaved := false;
    bool isConsistent := true;
    bool isInitialized := false;

    int stepEnabled := false;
    bool getEnabled[MaxNOutputs] := """),format.raw/*1248.37*/("""{"""),format.raw/*1248.38*/(""" """),_display_(/*1248.40*/m/*1248.41*/.getEnabled),format.raw/*1248.52*/(""" """),format.raw/*1248.53*/("""}"""),format.raw/*1248.54*/(""";
    bool setEnabled[MaxNInputs] := """),format.raw/*1249.36*/("""{"""),format.raw/*1249.37*/(""" """),_display_(/*1249.39*/m/*1249.40*/.setEnabled),format.raw/*1249.51*/(""" """),format.raw/*1249.52*/("""}"""),format.raw/*1249.53*/(""";

    void initialize()"""),format.raw/*1251.22*/("""{"""),format.raw/*1251.23*/("""
        """),format.raw/*1252.9*/("""isInitialized := true;
        //Set all variables to undefined
        for(i = 0; i &lt; nInput; i++)"""),format.raw/*1254.39*/("""{"""),format.raw/*1254.40*/("""
            """),format.raw/*1255.13*/("""inputVariables[i].status := undefined;
            inputVariables[i].time := 0;
        """),format.raw/*1257.9*/("""}"""),format.raw/*1257.10*/("""
        """),format.raw/*1258.9*/("""for(i := 0; i &lt; nOutput; i++)"""),format.raw/*1258.41*/("""{"""),format.raw/*1258.42*/("""
            """),format.raw/*1259.13*/("""outputVariables[i].status := undefined;
            outputVariables[i].time := 0;
        """),format.raw/*1261.9*/("""}"""),format.raw/*1261.10*/("""
    """),format.raw/*1262.5*/("""}"""),format.raw/*1262.6*/("""

    """),format.raw/*1264.5*/("""void getValue(int v, int a)"""),format.raw/*1264.32*/("""{"""),format.raw/*1264.33*/("""
        """),format.raw/*1265.9*/("""outputVariables[v].status := defined;
        outputVariables[v].time := cTime;

        connectionVariable[id][v][a].status := defined;
        connectionVariable[id][v][a].time := cTime;
    """),format.raw/*1270.5*/("""}"""),format.raw/*1270.6*/("""

    """),format.raw/*1272.5*/("""void setValue(int v, int a)"""),format.raw/*1272.32*/("""{"""),format.raw/*1272.33*/("""
        """),format.raw/*1273.9*/("""inputVariables[v].status := defined;
        for(i = 0; i &lt; nExternal; i++)"""),format.raw/*1274.42*/("""{"""),format.raw/*1274.43*/("""
            """),format.raw/*1275.13*/("""if(external[currentConfig][i].TrgFMU == id &amp;&amp; external[currentConfig][i].input == v)"""),format.raw/*1275.105*/("""{"""),format.raw/*1275.106*/("""
                """),format.raw/*1276.17*/("""inputVariables[v].time := connectionVariable[external[currentConfig][i].SrcFMU][external[currentConfig][i].output][a].time;
            """),format.raw/*1277.13*/("""}"""),format.raw/*1277.14*/("""
        """),format.raw/*1278.9*/("""}"""),format.raw/*1278.10*/("""
    """),format.raw/*1279.5*/("""}"""),format.raw/*1279.6*/("""

    """),format.raw/*1281.5*/("""//Proceed in time - we will start by assuming an FMU can't reject a stepsize
    void doStep(int t)"""),format.raw/*1282.23*/("""{"""),format.raw/*1282.24*/("""
        """),format.raw/*1283.9*/("""//Checking of step is valid
        if(t &gt; stepVariables[id])"""),format.raw/*1284.37*/("""{"""),format.raw/*1284.38*/("""
        """),format.raw/*1285.9*/("""//Step is too big and will not be allowed - t is reset too the biggest allowed step
            t := stepVariables[id];
        """),format.raw/*1287.9*/("""}"""),format.raw/*1287.10*/("""

        """),format.raw/*1289.9*/("""//Take step
        cTime := cTime + t;

        isConsistent := true;

        for(i = 0; i &lt; nInput; i++)"""),format.raw/*1294.39*/("""{"""),format.raw/*1294.40*/("""
            """),format.raw/*1295.13*/("""if(inputVariables[i].time != cTime)"""),format.raw/*1295.48*/("""{"""),format.raw/*1295.49*/("""
                """),format.raw/*1296.17*/("""isConsistent := false;
            """),format.raw/*1297.13*/("""}"""),format.raw/*1297.14*/("""
        """),format.raw/*1298.9*/("""}"""),format.raw/*1298.10*/("""

        """),format.raw/*1300.9*/("""//Reset outputs accesssed and advance their timestamp
        for(i = 0; i &lt; nOutput; i++)"""),format.raw/*1301.40*/("""{"""),format.raw/*1301.41*/("""
            """),format.raw/*1302.13*/("""//The inputs of the FMUs are inconsistent (not all are at time cTime) - so the FMUs output valid should be set to NaN
            if(isConsistent)"""),format.raw/*1303.29*/("""{"""),format.raw/*1303.30*/("""
                """),format.raw/*1304.17*/("""outputVariables[i].status := undefined;
                outputVariables[i].time := cTime;
            """),format.raw/*1306.13*/("""}"""),format.raw/*1306.14*/("""else"""),format.raw/*1306.18*/("""{"""),format.raw/*1306.19*/("""
                """),format.raw/*1307.17*/("""outputVariables[i].status := notStable;
                outputVariables[i].time := cTime;
            """),format.raw/*1309.13*/("""}"""),format.raw/*1309.14*/("""
        """),format.raw/*1310.9*/("""}"""),format.raw/*1310.10*/("""

        """),format.raw/*1312.9*/("""isConsistent := true;

        //Update or return the taken step size
        stepVariables[id] := t;
    """),format.raw/*1316.5*/("""}"""),format.raw/*1316.6*/("""

    """),format.raw/*1318.5*/("""void restoreFMU()"""),format.raw/*1318.22*/("""{"""),format.raw/*1318.23*/("""
        """),format.raw/*1319.9*/("""outputVariables := savedOutputVariables;
        inputVariables := savedInputVariables;
        cTime := savedTime;
    """),format.raw/*1322.5*/("""}"""),format.raw/*1322.6*/("""

    """),format.raw/*1324.5*/("""void saveFMU()"""),format.raw/*1324.19*/("""{"""),format.raw/*1324.20*/("""
        """),format.raw/*1325.9*/("""savedOutputVariables := outputVariables;
        savedInputVariables := inputVariables;
        savedTime := cTime;
        isSaved := true;
    """),format.raw/*1329.5*/("""}"""),format.raw/*1329.6*/("""

    """),format.raw/*1331.5*/("""bool preSetInit(int v, int a)"""),format.raw/*1331.34*/("""{"""),format.raw/*1331.35*/("""
        """),format.raw/*1332.9*/("""if(checksDisabled)"""),format.raw/*1332.27*/("""{"""),format.raw/*1332.28*/("""
            """),format.raw/*1333.13*/("""return true;
        """),format.raw/*1334.9*/("""}"""),format.raw/*1334.10*/("""
        """),format.raw/*1335.9*/("""//All outputs connected to the input should be defined - no difference between delay and reactive in init. ConnectionVariables an d ExternalConnections are having the same order
        return forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v imply
            connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined;

    """),format.raw/*1339.5*/("""}"""),format.raw/*1339.6*/("""

    """),format.raw/*1341.5*/("""bool preGetInit(int v)"""),format.raw/*1341.27*/("""{"""),format.raw/*1341.28*/("""
        """),format.raw/*1342.9*/("""if(checksDisabled)"""),format.raw/*1342.27*/("""{"""),format.raw/*1342.28*/("""
            """),format.raw/*1343.13*/("""return true;
        """),format.raw/*1344.9*/("""}"""),format.raw/*1344.10*/("""
        """),format.raw/*1345.9*/("""//The internal time should be equivalent to 0 and all variable connected to this one should be defined
        return forall(x:int[0, nInternalInit-1]) feedthroughInInit[x].FMU == id &amp;&amp; feedthroughInInit[x].output == v
            imply inputVariables[feedthroughInInit[x].input].status == defined;
    """),format.raw/*1348.5*/("""}"""),format.raw/*1348.6*/("""


    """),format.raw/*1351.5*/("""bool preSet(int v, int a)"""),format.raw/*1351.30*/("""{"""),format.raw/*1351.31*/("""
    """),format.raw/*1352.5*/("""if(checksDisabled)"""),format.raw/*1352.23*/("""{"""),format.raw/*1352.24*/("""
    """),format.raw/*1353.5*/("""return true;
    """),format.raw/*1354.5*/("""}"""),format.raw/*1354.6*/("""

    """),format.raw/*1356.5*/("""//If the connection is reactive the connected variable needs to have a greater than the time of the FMU and be defined
    return (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp;
    inputType[currentConfig][v] == reactive imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time &gt; cTime) &amp;&amp;
    (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp; inputType[currentConfig][v] == delayed
    imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time == cTime);
    """),format.raw/*1363.5*/("""}"""),format.raw/*1363.6*/("""


    """),format.raw/*1366.5*/("""bool preGet(int v)"""),format.raw/*1366.23*/("""{"""),format.raw/*1366.24*/("""
        """),format.raw/*1367.9*/("""if(checksDisabled)"""),format.raw/*1367.27*/("""{"""),format.raw/*1367.28*/("""
            """),format.raw/*1368.13*/("""return true;
        """),format.raw/*1369.9*/("""}"""),format.raw/*1369.10*/("""

        """),format.raw/*1371.9*/("""//All internal connections should be defined at time cTime
        return forall(x:int[0, nInternal-1]) feedthroughInStep[currentConfig][x].FMU == id &amp;&amp; feedthroughInStep[currentConfig][x].output == v
            imply inputVariables[feedthroughInStep[currentConfig][x].input].status == defined &amp;&amp; inputVariables[feedthroughInStep[currentConfig][x].input].time == cTime;
    """),format.raw/*1374.5*/("""}"""),format.raw/*1374.6*/("""

    """),format.raw/*1376.5*/("""bool preDoStep(int t)"""),format.raw/*1376.26*/("""{"""),format.raw/*1376.27*/("""
        """),format.raw/*1377.9*/("""if(checksDisabled)"""),format.raw/*1377.27*/("""{"""),format.raw/*1377.28*/("""
            """),format.raw/*1378.13*/("""return true;
        """),format.raw/*1379.9*/("""}"""),format.raw/*1379.10*/("""

        """),format.raw/*1381.9*/("""//All delayed input ports should be defined at the current time
        //And all reactive inputs ports should be defined at the next time step
        return (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == reactive imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime + t) &amp;&amp;
            (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == delayed imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime);
    """),format.raw/*1385.5*/("""}"""),format.raw/*1385.6*/("""

        """),format.raw/*1387.9*/("""//An FMU can only enter the Simulation mode when all connected FMU variables are defined at time 0
    bool preSimulation()"""),format.raw/*1388.25*/("""{"""),format.raw/*1388.26*/("""
        """),format.raw/*1389.9*/("""return ((forall(x:int[0, MaxNOutputs-1]) outputVariables[x].status == defined &amp;&amp; outputVariables[x].time == 0)
        &amp;&amp; (forall(x:int[0, MaxNInputs-1]) inputVariables[x].status == defined &amp;&amp;
        inputVariables[x].time == 0));
    """),format.raw/*1392.5*/("""}"""),format.raw/*1392.6*/("""

    """),format.raw/*1394.5*/("""bool preSaveFMU()"""),format.raw/*1394.22*/("""{"""),format.raw/*1394.23*/("""
        """),format.raw/*1395.9*/("""//Always possible
        return true;
    """),format.raw/*1397.5*/("""}"""),format.raw/*1397.6*/("""

    """),format.raw/*1399.5*/("""bool preRestoreFMU()"""),format.raw/*1399.25*/("""{"""),format.raw/*1399.26*/("""
        """),format.raw/*1400.9*/("""//Should a requirement be a saved previous FMU?
        return isSaved;
    """),format.raw/*1402.5*/("""}"""),format.raw/*1402.6*/("""

    """),format.raw/*1404.5*/("""void updateEnableActions()"""),format.raw/*1404.31*/("""{"""),format.raw/*1404.32*/("""
        """),format.raw/*1405.9*/("""if(!isInitialized)"""),format.raw/*1405.27*/("""{"""),format.raw/*1405.28*/("""
            """),format.raw/*1406.13*/("""for(i = 0; i &lt; nInput; i++)"""),format.raw/*1406.43*/("""{"""),format.raw/*1406.44*/("""
                """),format.raw/*1407.17*/("""setEnabled[i] := preSetInit(i, final) &amp;&amp;
                inputVariables[i].status == undefined;
            """),format.raw/*1409.13*/("""}"""),format.raw/*1409.14*/("""
            """),format.raw/*1410.13*/("""for(i := 0; i &lt; nOutput; i++)"""),format.raw/*1410.45*/("""{"""),format.raw/*1410.46*/("""
                """),format.raw/*1411.17*/("""getEnabled[i] := preGetInit(i) &amp;&amp;
                outputVariables[i].status == undefined;
            """),format.raw/*1413.13*/("""}"""),format.raw/*1413.14*/("""
            """),format.raw/*1414.13*/("""stepEnabled := false;
        """),format.raw/*1415.9*/("""}"""),format.raw/*1415.10*/("""else"""),format.raw/*1415.14*/("""{"""),format.raw/*1415.15*/("""
            """),format.raw/*1416.13*/("""for(i = 0; i &lt; nInput; i++)"""),format.raw/*1416.43*/("""{"""),format.raw/*1416.44*/("""
                """),format.raw/*1417.17*/("""setEnabled[i] := preSet(i, final);
            """),format.raw/*1418.13*/("""}"""),format.raw/*1418.14*/("""
            """),format.raw/*1419.13*/("""for(i := 0; i &lt; nOutput; i++)"""),format.raw/*1419.45*/("""{"""),format.raw/*1419.46*/("""
                """),format.raw/*1420.17*/("""getEnabled[i] := preGet(i);
            """),format.raw/*1421.13*/("""}"""),format.raw/*1421.14*/("""
            """),format.raw/*1422.13*/("""stepEnabled := preDoStep(h);
        """),format.raw/*1423.9*/("""}"""),format.raw/*1423.10*/("""
    """),format.raw/*1424.5*/("""}"""),format.raw/*1424.6*/("""

"""),format.raw/*1426.1*/("""</declaration>
<location id="id45" x="-10285" y="-11662">
    <committed/>
</location>
<location id="id46" x="-11058" y="-11509">
    <name x="-11109" y="-11560">ActionPerformed_Init</name>
    <committed/>
</location>
<location id="id47" x="-9800" y="-10990">
    <name x="-9810" y="-11024">Unloaded</name>
</location>
<location id="id48" x="-10030" y="-10990">
    <name x="-10081" y="-11033">Instance_Freed</name>
</location>
<location id="id49" x="-11584" y="-11118">
</location>
<location id="id50" x="-12034" y="-11118">
    <committed/>
</location>
<location id="id51" x="-10752" y="-11305">
    <label kind="invariant" x="-10795" y="-11279">preSimulation()</label>
    <committed/>
</location>
<location id="id52" x="-11339" y="-11305">
    <committed/>
</location>
<location id="id53" x="-11813" y="-11305">
    <committed/>
</location>
<location id="id54" x="-12230" y="-11305">
    <committed/>
</location>
<location id="id55" x="-11584" y="-11305">
    <name x="-11645" y="-11381">Experiment_Setup</name>
</location>
<location id="id56" x="-12034" y="-11305">
    <name x="-12078" y="-11390">Instantiated</name>
</location>
<location id="id57" x="-10183" y="-11475">
    <label kind="invariant" x="-10174" y="-11458">preRestoreFMU()</label>
    <committed/>
</location>
<location id="id58" x="-10948" y="-11415">
    <label kind="invariant" x="-10922" y="-11432">preGetInit(var)</label>
    <committed/>
</location>
<location id="id59" x="-11177" y="-11415">
    <label kind="invariant" x="-11381" y="-11441">preSetInit(var, commitment)</label>
    <committed/>
</location>
<location id="id60" x="-12418" y="-11305">
    <name x="-12477" y="-11288">loaded</name>
</location>
<location id="id61" x="-10030" y="-11373">
    <label kind="invariant" x="-10047" y="-11407">preSaveFMU()</label>
    <committed/>
</location>
<location id="id62" x="-10336" y="-11492">
    <label kind="invariant" x="-10370" y="-11466">preDoStep(stepsize)</label>
    <committed/>
</location>
<location id="id63" x="-10523" y="-11500">
    <label kind="invariant" x="-10599" y="-11492">preGet(var)</label>
    <committed/>
</location>
<location id="id64" x="-11058" y="-11305">
    <name x="-11050" y="-11296">Initialize</name>
</location>
<location id="id65" x="-10642" y="-11441">
    <label kind="invariant" x="-10812" y="-11492">preSet(var, commitment)</label>
    <committed/>
</location>
<location id="id66" x="-10387" y="-10990">
</location>
<location id="id67" x="-10387" y="-11305">
    <name x="-10498" y="-11330">Simulation</name>
</location>
<init ref="id60"/>
<transition>
    <source ref="id45"/>
    <target ref="id67"/>
    <label kind="synchronisation" x="-10183" y="-11679">actionPerformed!</label>
    <label kind="assignment" x="-9928" y="-11560">updateEnableActions()</label>
    <nail x="-9936" y="-11653"/>
    <nail x="-9936" y="-11313"/>
</transition>
<transition>
    <source ref="id61"/>
    <target ref="id45"/>
    <label kind="assignment" x="-10149" y="-11602">saveFMU()</label>
    <nail x="-10038" y="-11526"/>
</transition>
<transition>
    <source ref="id57"/>
    <target ref="id45"/>
    <label kind="assignment" x="-10259" y="-11526">restoreFMU()</label>
</transition>
<transition>
    <source ref="id62"/>
    <target ref="id45"/>
    <label kind="assignment" x="-10361" y="-11560">doStep(stepsize)</label>
</transition>
<transition>
    <source ref="id63"/>
    <target ref="id45"/>
    <label kind="assignment" x="-10557" y="-11577">getValue(var, commitment)</label>
</transition>
<transition>
    <source ref="id65"/>
    <target ref="id45"/>
    <label kind="assignment" x="-10565" y="-11679">setValue(var, commitment)</label>
    <nail x="-10582" y="-11645"/>
</transition>
<transition>
    <source ref="id58"/>
    <target ref="id46"/>
    <label kind="assignment" x="-10990" y="-11534">getValue(var, commitment)</label>
</transition>
<transition>
    <source ref="id46"/>
    <target ref="id64"/>
    <label kind="synchronisation" x="-11118" y="-11449">actionPerformed!</label>
    <label kind="assignment" x="-11143" y="-11415">updateEnableActions()</label>
</transition>
<transition>
    <source ref="id59"/>
    <target ref="id46"/>
    <label kind="assignment" x="-11288" y="-11509">setValue(var, commitment)</label>
</transition>
<transition>
    <source ref="id48"/>
    <target ref="id47"/>
    <label kind="guard" x="-9979" y="-11016">action == unload</label>
    <label kind="synchronisation" x="-9936" y="-10973">fmu[id]?</label>
</transition>
<transition>
    <source ref="id64"/>
    <target ref="id64"/>
    <label kind="synchronisation" x="-11101" y="-11152">actionPerformed?</label>
    <label kind="assignment" x="-11126" y="-11109">updateEnableActions()</label>
    <label kind="comments" x="-11152" y="-11058">Other SUs have performed an action</label>
    <nail x="-10999" y="-11169"/>
    <nail x="-11126" y="-11169"/>
</transition>
<transition>
    <source ref="id66"/>
    <target ref="id48"/>
    <label kind="guard" x="-10259" y="-11016">action == freeInstance</label>
    <label kind="synchronisation" x="-10242" y="-10973">fmu[id]?</label>
</transition>
<transition>
    <source ref="id51"/>
    <target ref="id67"/>
    <label kind="synchronisation" x="-10693" y="-11322">actionPerformed!</label>
    <label kind="assignment" x="-10727" y="-11347">updateEnableActions()</label>
</transition>
<transition>
    <source ref="id64"/>
    <target ref="id51"/>
    <label kind="guard" x="-10973" y="-11305">action == exitInitialization</label>
    <label kind="synchronisation" x="-10939" y="-11331">fmu[id]?</label>
</transition>
<transition>
    <source ref="id52"/>
    <target ref="id64"/>
    <label kind="synchronisation" x="-11287" y="-11330">actionPerformed!</label>
    <label kind="assignment" x="-11322" y="-11296">updateEnableActions(),
        initialize()</label>
</transition>
<transition>
    <source ref="id55"/>
    <target ref="id52"/>
    <label kind="guard" x="-11559" y="-11356">action == enterInitialization</label>
    <label kind="synchronisation" x="-11542" y="-11331">fmu[id]?</label>
</transition>
<transition>
    <source ref="id49"/>
    <target ref="id55"/>
    <label kind="synchronisation" x="-11771" y="-11245">actionPerformed!</label>
    <nail x="-11660" y="-11211"/>
</transition>
<transition>
    <source ref="id55"/>
    <target ref="id49"/>
    <label kind="guard" x="-11533" y="-11160">action == setParameter</label>
    <label kind="synchronisation" x="-11516" y="-11186">fmu[id]?</label>
    <nail x="-11507" y="-11211"/>
</transition>
<transition>
    <source ref="id50"/>
    <target ref="id56"/>
    <label kind="synchronisation" x="-12255" y="-11169">actionPerformed!</label>
    <nail x="-12136" y="-11211"/>
</transition>
<transition>
    <source ref="id56"/>
    <target ref="id50"/>
    <label kind="guard" x="-11958" y="-11152">action == setParameter</label>
    <label kind="synchronisation" x="-11924" y="-11211">fmu[id]?</label>
    <nail x="-11932" y="-11203"/>
</transition>
<transition>
    <source ref="id53"/>
    <target ref="id55"/>
    <label kind="synchronisation" x="-11779" y="-11330">actionPerformed!</label>
</transition>
<transition>
    <source ref="id56"/>
    <target ref="id53"/>
    <label kind="guard" x="-12000" y="-11339">action == setupExperiment</label>
    <label kind="synchronisation" x="-11992" y="-11288">fmu[id]?</label>
</transition>
<transition>
    <source ref="id54"/>
    <target ref="id56"/>
    <label kind="synchronisation" x="-12204" y="-11330">actionPerformed!</label>
</transition>
<transition>
    <source ref="id60"/>
    <target ref="id54"/>
    <label kind="guard" x="-12400" y="-11339">action == instantiate</label>
    <label kind="synchronisation" x="-12382" y="-11305">fmu[id]?</label>
</transition>
<transition>
    <source ref="id67"/>
    <target ref="id57"/>
    <label kind="guard" x="-10259" y="-11390">action == restore</label>
    <label kind="synchronisation" x="-10302" y="-11373">fmu[id]?</label>
    <nail x="-10200" y="-11390"/>
</transition>
<transition>
    <source ref="id67"/>
    <target ref="id67"/>
    <label kind="synchronisation" x="-10633" y="-11203">actionPerformed?</label>
    <label kind="assignment" x="-10659" y="-11177">updateEnableActions()</label>
    <nail x="-10438" y="-11143"/>
    <nail x="-10557" y="-11237"/>
</transition>
<transition>
    <source ref="id64"/>
    <target ref="id58"/>
    <label kind="guard" x="-10973" y="-11390">action == get</label>
    <label kind="synchronisation" x="-10982" y="-11373">fmu[id]?</label>
    <nail x="-10990" y="-11373"/>
</transition>
<transition>
    <source ref="id64"/>
    <target ref="id59"/>
    <label kind="guard" x="-11254" y="-11398">action == set</label>
    <label kind="synchronisation" x="-11254" y="-11415">fmu[id]?</label>
    <nail x="-11135" y="-11373"/>
</transition>
<transition>
    <source ref="id67"/>
    <target ref="id61"/>
    <label kind="guard" x="-10234" y="-11339">action == save</label>
    <label kind="synchronisation" x="-10140" y="-11356">fmu[id]?</label>
</transition>
<transition>
    <source ref="id67"/>
    <target ref="id62"/>
    <label kind="guard" x="-10369" y="-11415">action == step</label>
    <label kind="synchronisation" x="-10361" y="-11398">fmu[id]?</label>
</transition>
<transition>
    <source ref="id67"/>
    <target ref="id63"/>
    <label kind="guard" x="-10489" y="-11475">action == get</label>
    <label kind="synchronisation" x="-10463" y="-11449">fmu[id]?</label>
</transition>
<transition>
    <source ref="id67"/>
    <target ref="id65"/>
    <label kind="guard" x="-10608" y="-11441">action == set</label>
    <label kind="synchronisation" x="-10633" y="-11415">fmu[id]?</label>
</transition>
<transition>
    <source ref="id67"/>
    <target ref="id66"/>
    <label kind="guard" x="-10378" y="-11101">action == terminate</label>
    <label kind="synchronisation" x="-10378" y="-11126">fmu[id]?</label>
</transition>
</template>
    <system>
        // Place template instantiations here.
        MasterA = Interpreter();

        //Max number of tries in the loops is upper bounded by the number of FMUs
        loopS = LoopSolver(nFMU + 1);
        finder = StepFinder(H_max + 1);
        loop_solver_init = LoopSolverInit(nFMU + 1);


        //The arguments to FMU is Id, numbers of outputs, number of inputs, definition of inputTypes
        """),_display_(/*1720.10*/for(fName<- m.fmuNames) yield /*1720.33*/ {_display_(Seq[Any](format.raw/*1720.35*/("""
        """),_display_(/*1721.10*/{fName}),format.raw/*1721.17*/("""_fmu = FMU("""),_display_(/*1721.29*/{fName}),format.raw/*1721.36*/(""", """),_display_(/*1721.39*/{fName}),format.raw/*1721.46*/("""_output, """),_display_(/*1721.56*/{fName}),format.raw/*1721.63*/("""_input, """),_display_(/*1721.72*/{fName}),format.raw/*1721.79*/("""_inputTypes) ;
        """)))}),format.raw/*1722.10*/("""

        """),format.raw/*1724.9*/("""// List one or more processes to be composed into a system.
        system MasterA,
        """),_display_(/*1726.10*/{m.fmuNames.map(fName => s"${fName}_fmu").reduce[String]((a, b) => a + "," + b)}),format.raw/*1726.90*/(""",
        loopS, finder, loop_solver_init;
    </system>
    <queries>
        <query>
            <formula>A[] not deadlock
            </formula>
            <comment>
            </comment>
        </query>
        <query>
            <formula>A&lt;&gt; MasterA.Terminated
            </formula>
            <comment>
            </comment>
        </query>
    </queries>
</nta>

"""))
      }
    }
  }

  def render(m:ModelEncoding): play.twirl.api.XmlFormat.Appendable = apply(m)

  def f:((ModelEncoding) => play.twirl.api.XmlFormat.Appendable) = (m) => apply(m)

  def ref: this.type = this

}


              /*
                  -- GENERATED --
                  SOURCE: src/main/twirl/FMI3CosimUppaalTemplate.scala.xml
                  HASH: 976555486d04d495cb793f84cc0f58f6af9b53a8
                  MATRIX: 262->1|644->66|755->85|789->93|2176->1452|2205->1453|2246->1466|2390->1582|2419->1583|2464->1600|2518->1626|2547->1627|2589->1641|2697->1721|2726->1722|2771->1739|2936->1876|2965->1877|3007->1891|3196->2052|3225->2053|3270->2070|3323->2095|3352->2096|3394->2110|3443->2132|3472->2133|3509->2143|3711->2317|3740->2318|3781->2331|3856->2379|3885->2380|3914->2381|4350->2790|4360->2791|4392->2802|4454->2837|4464->2838|4497->2850|4611->2937|4621->2938|4648->2944|4790->3059|4800->3060|4843->3082|4916->3128|4926->3129|4969->3151|5100->3255|5110->3256|5140->3265|5568->3666|5578->3667|5604->3672|5785->3824|5815->3825|5857->3838|6143->4096|6173->4097|6203->4098|6267->4133|6297->4134|6339->4147|6473->4253|6503->4254|6533->4255|6735->4428|6765->4429|6807->4442|6972->4579|7002->4580|7032->4581|7105->4625|7135->4626|7177->4639|7270->4704|7300->4705|7330->4706|9513->6861|9524->6862|9556->6872|9622->6910|9633->6911|9669->6925|9785->7013|9796->7014|9828->7024|10074->7241|10104->7243|10115->7244|10151->7258|10181->7259|10313->7362|10343->7363|10385->7376|10911->7873|10941->7874|10987->7891|11058->7933|11088->7934|11130->7947|11177->7965|11207->7966|11253->7983|11306->8007|11336->8008|11386->8029|11563->8177|11593->8178|11623->8179|11676->8203|11706->8204|11756->8225|11852->8292|11882->8293|11912->8294|11946->8299|11976->8300|12026->8321|12131->8397|12161->8398|12203->8411|12233->8412|12275->8425|12387->8509|12417->8510|12456->8521|12582->8619|12622->8642|12663->8644|12700->8653|12739->8664|12766->8669|12796->8670|12828->8674|12839->8675|12874->8688|12917->8699|12955->8709|13035->8761|13075->8784|13116->8786|13153->8795|13192->8806|13221->8813|13260->8824|13271->8825|13308->8840|13357->8861|13386->8868|13426->8880|13437->8881|13475->8897|13518->8908|13556->8918|13640->8974|13680->8997|13721->8999|13758->9008|13790->9012|13817->9017|13847->9018|13885->9028|13896->9029|13933->9044|13971->9054|14021->9087|14062->9089|14099->9098|14138->9109|14190->9139|14220->9140|14252->9144|14263->9145|14297->9169|14327->9177|14370->9188|14407->9197|14439->9201|14466->9206|14496->9207|14535->9218|14546->9219|14584->9235|14622->9245|14674->9280|14715->9282|14752->9291|14791->9302|14844->9333|14874->9334|14906->9338|14917->9339|14952->9364|14983->9373|15026->9384|15063->9393|15102->9404|15131->9411|15196->9447|15226->9448|15256->9450|15267->9451|15310->9472|15340->9473|15370->9474|15413->9485|15451->9495|15732->9747|15762->9748|15792->9750|15803->9751|15844->9770|15874->9771|15904->9772|16100->9939|16130->9940|16160->9942|16171->9943|16202->9952|16232->9953|16262->9954|16367->10030|16397->10031|16427->10033|16438->10034|16478->10052|16508->10053|16538->10054|16775->10262|16805->10263|16835->10265|16846->10266|16886->10284|16916->10285|16946->10286|17158->10469|17188->10470|17218->10472|17229->10473|17265->10487|17295->10488|17325->10489|17397->10533|17408->10534|17448->10552|17571->10647|17582->10648|17629->10673|17707->10723|17718->10724|17766->10750|17864->10819|17894->10821|17905->10822|17943->10838|17973->10839|18048->10886|18059->10887|18104->10910|18248->11026|18259->11027|18315->11061|18413->11131|18424->11132|18493->11178|18733->11388|18764->11390|18776->11391|18839->11431|18870->11432|19016->11548|19047->11550|19059->11551|19116->11585|19147->11586|19281->11692|19292->11693|19336->11715|19417->11768|19428->11769|19479->11798|19586->11876|19616->11878|19627->11879|19669->11899|19700->11900|19802->11973|19832->11975|19843->11976|19874->11985|19904->11986|20048->12102|20059->12103|20115->12137|20210->12204|20221->12205|20287->12248|20385->12318|20396->12319|20465->12365|20714->12584|20745->12586|20757->12587|20820->12627|20851->12628|21006->12753|21037->12755|21049->12756|21106->12790|21137->12791|21306->12930|21337->12932|21349->12933|21411->12972|21442->12973|21683->13185|21713->13186|21743->13188|21754->13189|21801->13213|21832->13214|21863->13215|21973->13296|22003->13297|22033->13299|22044->13300|22092->13325|22123->13326|22154->13327|22255->13399|22285->13400|22315->13402|22326->13403|22363->13418|22393->13419|22423->13420|23005->13973|23035->13974|23065->13976|23076->13977|23122->14000|23153->14001|23184->14002|23288->14077|23318->14078|23348->14080|23359->14081|23412->14111|23443->14112|23474->14113|23752->14361|23783->14363|23795->14364|23851->14397|23882->14398|23913->14399|24062->14518|24093->14520|24105->14521|24161->14554|24192->14555|24223->14556|24626->14929|24657->14931|24669->14932|24731->14971|24762->14972|24793->14973|24964->15114|24995->15116|25007->15117|25069->15156|25100->15157|25131->15158|25304->15301|25335->15303|25347->15304|25408->15342|25439->15343|25470->15344|25574->15419|25604->15420|25634->15422|25645->15423|25689->15445|25720->15446|25751->15447|25998->15665|26028->15666|26065->15675|26200->15782|26229->15783|26263->15789|26319->15816|26349->15817|26386->15826|26522->15934|26551->15935|26586->15942|26647->15974|26677->15975|26714->15984|26795->16036|26825->16037|26867->16050|26965->16120|26995->16121|27032->16130|27113->16183|27142->16184|27176->16190|27234->16219|27264->16220|27301->16229|27444->16344|27473->16345|27508->16352|27579->16394|27609->16395|27646->16404|27920->16650|27949->16651|27983->16657|28059->16704|28089->16705|28126->16714|28177->16736|28207->16737|28249->16750|28418->16891|28448->16892|28481->16896|28511->16897|28553->16910|28679->17008|28709->17009|28746->17018|28783->17027|28812->17028|28841->17029|37793->25952|37823->25953|37860->25962|38022->26096|38051->26097|38085->26103|38152->26141|38182->26142|38219->26151|38346->26249|38376->26250|38418->26263|38775->26592|38805->26593|38838->26598|38867->26599|38901->26605|38949->26624|38979->26625|39016->26634|39231->26821|39260->26822|39295->26829|39351->26856|39381->26857|39418->26866|39513->26933|39542->26934|39576->26940|39736->27071|39766->27072|39803->27081|40589->27839|40618->27840|40652->27846|40704->27869|40734->27870|40771->27879|40868->27948|40897->27949|40932->27956|40986->27981|41016->27982|41053->27991|41108->28017|41138->28018|41180->28031|41246->28069|41276->28070|41309->28075|41338->28076|41367->28077|45030->31711|45060->31712|45097->31721|45274->31870|45303->31871|45337->31877|45401->31912|45431->31913|45468->31922|45608->32034|45637->32035|45672->32042|45739->32080|45769->32081|45806->32090|45967->32222|45997->32223|46039->32236|46250->32418|46280->32419|46326->32436|46559->32640|46589->32641|46622->32645|46652->32646|46698->32663|46931->32867|46961->32868|46998->32877|47028->32878|47061->32883|47090->32884|47124->32890|47172->32909|47202->32910|47239->32919|47536->33188|47565->33189|47600->33196|47656->33223|47686->33224|47723->33233|47843->33325|47872->33326|47906->33332|48066->33463|48096->33464|48133->33473|49090->34402|49119->34403|49153->34409|49205->34432|49235->34433|49272->34442|49369->34511|49398->34512|49433->34519|49487->34544|49517->34545|49554->34554|49609->34580|49639->34581|49681->34594|49747->34632|49777->34633|49810->34638|49839->34639|49868->34640|55468->40210|55499->40211|55537->40220|55649->40303|55679->40304|55714->40310|55778->40344|55809->40345|55847->40354|55955->40433|55985->40434|56020->40440|56068->40458|56099->40459|56137->40468|56275->40576|56306->40577|56349->40590|56408->40619|56439->40620|56486->40637|56553->40674|56584->40675|56622->40684|56653->40685|56691->40694|56734->40708|56764->40709|56800->40716|56846->40732|56877->40733|56915->40742|57108->40906|57138->40907|57173->40913|57223->40933|57254->40934|57292->40943|57379->41001|57409->41002|57445->41009|57495->41029|57526->41030|57564->41039|57609->41054|57640->41055|57683->41068|57922->41278|57953->41279|57987->41284|58017->41285|58047->41286|62800->46009|62831->46010|62862->46012|62916->46043|62947->46044|62978->46045|63054->46091|63085->46092|63116->46094|63171->46126|63202->46127|63233->46128|63600->46465|63631->46466|63662->46468|63674->46469|63708->46480|63739->46481|63770->46482|63837->46519|63868->46520|63899->46522|63911->46523|63945->46534|63976->46535|64007->46536|64061->46560|64092->46561|64130->46570|64262->46672|64293->46673|64336->46686|64453->46774|64484->46775|64522->46784|64584->46816|64615->46817|64658->46830|64777->46920|64808->46921|64842->46926|64872->46927|64907->46933|64964->46960|64995->46961|65033->46970|65255->47163|65285->47164|65320->47170|65377->47197|65408->47198|65446->47207|65554->47285|65585->47286|65628->47299|65751->47391|65783->47392|65830->47409|65996->47545|66027->47546|66065->47555|66096->47556|66130->47561|66160->47562|66195->47568|66324->47667|66355->47668|66393->47677|66487->47741|66518->47742|66556->47751|66713->47879|66744->47880|66783->47890|66923->48000|66954->48001|66997->48014|67062->48049|67093->48050|67140->48067|67205->48102|67236->48103|67274->48112|67305->48113|67344->48123|67467->48216|67498->48217|67541->48230|67717->48376|67748->48377|67795->48394|67927->48496|67958->48497|67992->48501|68023->48502|68070->48519|68202->48621|68233->48622|68271->48631|68302->48632|68341->48642|68476->48748|68506->48749|68541->48755|68588->48772|68619->48773|68657->48782|68806->48902|68836->48903|68871->48909|68915->48923|68946->48924|68984->48933|69158->49078|69188->49079|69223->49085|69282->49114|69313->49115|69351->49124|69399->49142|69430->49143|69473->49156|69523->49177|69554->49178|69592->49187|70067->49633|70097->49634|70132->49640|70184->49662|70215->49663|70253->49672|70301->49690|70332->49691|70375->49704|70425->49725|70456->49726|70494->49735|70834->50046|70864->50047|70900->50054|70955->50079|70986->50080|71020->50085|71068->50103|71099->50104|71133->50109|71179->50126|71209->50127|71244->50133|72250->51110|72280->51111|72316->51118|72364->51136|72395->51137|72433->51146|72481->51164|72512->51165|72555->51178|72605->51199|72636->51200|72675->51210|73095->51601|73125->51602|73160->51608|73211->51629|73242->51630|73280->51639|73328->51657|73359->51658|73402->51671|73452->51692|73483->51693|73522->51703|74057->52209|74087->52210|74126->52220|74279->52343|74310->52344|74348->52353|74637->52613|74667->52614|74702->52620|74749->52637|74780->52638|74818->52647|74890->52690|74920->52691|74955->52697|75005->52717|75036->52718|75074->52727|75179->52803|75209->52804|75244->52810|75300->52836|75331->52837|75369->52846|75417->52864|75448->52865|75491->52878|75551->52908|75582->52909|75629->52926|75775->53042|75806->53043|75849->53056|75911->53088|75942->53089|75989->53106|76129->53216|76160->53217|76203->53230|76262->53260|76293->53261|76327->53265|76358->53266|76401->53279|76461->53309|76492->53310|76539->53327|76616->53374|76647->53375|76690->53388|76752->53420|76783->53421|76830->53438|76900->53478|76931->53479|76974->53492|77040->53529|77071->53530|77105->53535|77135->53536|77166->53538|87548->63891|87589->63914|87631->63916|87670->63926|87700->63933|87741->63945|87771->63952|87803->63955|87833->63962|87872->63972|87902->63979|87940->63988|87970->63995|88027->64019|88066->64029|88188->64122|88291->64202
                  LINES: 10->1|15->2|20->3|20->3|54->37|54->37|55->38|56->39|56->39|57->40|58->41|58->41|60->43|61->44|61->44|62->45|64->47|64->47|66->49|67->50|67->50|68->51|69->52|69->52|71->54|72->55|72->55|74->57|80->63|80->63|81->64|83->66|83->66|83->66|94->77|94->77|94->77|95->78|95->78|95->78|98->81|98->81|98->81|101->84|101->84|101->84|102->85|102->85|102->85|105->88|105->88|105->88|115->98|115->98|115->98|122->105|122->105|123->106|130->113|130->113|130->113|132->115|132->115|133->116|136->119|136->119|136->119|143->126|143->126|144->127|148->131|148->131|148->131|150->133|150->133|151->134|153->136|153->136|153->136|216->199|216->199|216->199|217->200|217->200|217->200|220->203|220->203|220->203|226->209|226->209|226->209|226->209|226->209|229->212|229->212|230->213|242->225|242->225|243->226|244->227|244->227|245->228|245->228|245->228|246->229|246->229|246->229|247->230|249->232|249->232|249->232|249->232|249->232|250->233|252->235|252->235|252->235|252->235|252->235|253->236|255->238|255->238|256->239|256->239|257->240|259->242|259->242|262->245|264->247|264->247|264->247|265->248|265->248|265->248|265->248|265->248|265->248|265->248|266->249|268->251|269->252|269->252|269->252|270->253|270->253|270->253|270->253|270->253|270->253|271->254|271->254|271->254|271->254|271->254|272->255|274->257|275->258|275->258|275->258|276->259|276->259|276->259|276->259|276->259|276->259|276->259|277->260|277->260|277->260|278->261|278->261|278->261|278->261|278->261|278->261|278->261|278->261|279->262|280->263|280->263|280->263|280->263|280->263|280->263|280->263|281->264|281->264|281->264|282->265|282->265|282->265|282->265|282->265|282->265|282->265|282->265|283->266|284->267|284->267|284->267|284->267|284->267|284->267|284->267|284->267|284->267|284->267|285->268|287->270|289->272|289->272|289->272|289->272|289->272|289->272|289->272|292->275|292->275|292->275|292->275|292->275|292->275|292->275|294->277|294->277|294->277|294->277|294->277|294->277|294->277|297->280|297->280|297->280|297->280|297->280|297->280|297->280|300->283|300->283|300->283|300->283|300->283|300->283|300->283|303->286|303->286|303->286|306->289|306->289|306->289|307->290|307->290|307->290|308->291|308->291|308->291|308->291|308->291|309->292|309->292|309->292|312->295|312->295|312->295|313->296|313->296|313->296|316->299|316->299|316->299|316->299|316->299|317->300|317->300|317->300|317->300|317->300|322->305|322->305|322->305|323->306|323->306|323->306|325->308|325->308|325->308|325->308|325->308|326->309|326->309|326->309|326->309|326->309|329->312|329->312|329->312|330->313|330->313|330->313|331->314|331->314|331->314|334->317|334->317|334->317|334->317|334->317|335->318|335->318|335->318|335->318|335->318|336->319|336->319|336->319|336->319|336->319|339->322|339->322|339->322|339->322|339->322|339->322|339->322|341->324|341->324|341->324|341->324|341->324|341->324|341->324|343->326|343->326|343->326|343->326|343->326|343->326|343->326|351->334|351->334|351->334|351->334|351->334|351->334|351->334|352->335|352->335|352->335|352->335|352->335|352->335|352->335|356->339|356->339|356->339|356->339|356->339|356->339|357->340|357->340|357->340|357->340|357->340|357->340|361->344|361->344|361->344|361->344|361->344|361->344|362->345|362->345|362->345|362->345|362->345|362->345|364->347|364->347|364->347|364->347|364->347|364->347|366->349|366->349|366->349|366->349|366->349|366->349|366->349|378->361|378->361|379->362|382->365|382->365|384->367|384->367|384->367|385->368|388->371|388->371|391->374|391->374|391->374|392->375|392->375|392->375|393->376|394->377|394->377|395->378|397->380|397->380|399->382|399->382|399->382|400->383|403->386|403->386|406->389|406->389|406->389|407->390|416->399|416->399|418->401|418->401|418->401|419->402|419->402|419->402|420->403|422->405|422->405|422->405|422->405|423->406|425->408|425->408|426->409|427->410|427->410|428->411|691->674|691->674|692->675|695->678|695->678|697->680|697->680|697->680|698->681|700->683|700->683|701->684|705->688|705->688|706->689|706->689|708->691|708->691|708->691|709->692|715->698|715->698|718->701|718->701|718->701|719->702|721->704|721->704|723->706|724->707|724->707|725->708|731->714|731->714|733->716|733->716|733->716|734->717|735->718|735->718|738->721|738->721|738->721|739->722|739->722|739->722|740->723|741->724|741->724|742->725|742->725|743->726|850->833|850->833|851->834|854->837|854->837|856->839|856->839|856->839|857->840|859->842|859->842|862->845|862->845|862->845|863->846|866->849|866->849|867->850|869->852|869->852|870->853|872->855|872->855|872->855|872->855|873->856|875->858|875->858|876->859|876->859|877->860|877->860|879->862|879->862|879->862|880->863|888->871|888->871|891->874|891->874|891->874|892->875|895->878|895->878|897->880|898->881|898->881|899->882|908->891|908->891|910->893|910->893|910->893|911->894|912->895|912->895|915->898|915->898|915->898|916->899|916->899|916->899|917->900|918->901|918->901|919->902|919->902|920->903|1072->1055|1072->1055|1073->1056|1075->1058|1075->1058|1077->1060|1077->1060|1077->1060|1078->1061|1080->1063|1080->1063|1082->1065|1082->1065|1082->1065|1083->1066|1086->1069|1086->1069|1087->1070|1087->1070|1087->1070|1088->1071|1089->1072|1089->1072|1090->1073|1090->1073|1091->1074|1092->1075|1092->1075|1095->1078|1095->1078|1095->1078|1096->1079|1098->1081|1098->1081|1100->1083|1100->1083|1100->1083|1101->1084|1102->1085|1102->1085|1105->1088|1105->1088|1105->1088|1106->1089|1106->1089|1106->1089|1107->1090|1110->1093|1110->1093|1111->1094|1111->1094|1112->1095|1250->1233|1250->1233|1250->1233|1250->1233|1250->1233|1250->1233|1251->1234|1251->1234|1251->1234|1251->1234|1251->1234|1251->1234|1265->1248|1265->1248|1265->1248|1265->1248|1265->1248|1265->1248|1265->1248|1266->1249|1266->1249|1266->1249|1266->1249|1266->1249|1266->1249|1266->1249|1268->1251|1268->1251|1269->1252|1271->1254|1271->1254|1272->1255|1274->1257|1274->1257|1275->1258|1275->1258|1275->1258|1276->1259|1278->1261|1278->1261|1279->1262|1279->1262|1281->1264|1281->1264|1281->1264|1282->1265|1287->1270|1287->1270|1289->1272|1289->1272|1289->1272|1290->1273|1291->1274|1291->1274|1292->1275|1292->1275|1292->1275|1293->1276|1294->1277|1294->1277|1295->1278|1295->1278|1296->1279|1296->1279|1298->1281|1299->1282|1299->1282|1300->1283|1301->1284|1301->1284|1302->1285|1304->1287|1304->1287|1306->1289|1311->1294|1311->1294|1312->1295|1312->1295|1312->1295|1313->1296|1314->1297|1314->1297|1315->1298|1315->1298|1317->1300|1318->1301|1318->1301|1319->1302|1320->1303|1320->1303|1321->1304|1323->1306|1323->1306|1323->1306|1323->1306|1324->1307|1326->1309|1326->1309|1327->1310|1327->1310|1329->1312|1333->1316|1333->1316|1335->1318|1335->1318|1335->1318|1336->1319|1339->1322|1339->1322|1341->1324|1341->1324|1341->1324|1342->1325|1346->1329|1346->1329|1348->1331|1348->1331|1348->1331|1349->1332|1349->1332|1349->1332|1350->1333|1351->1334|1351->1334|1352->1335|1356->1339|1356->1339|1358->1341|1358->1341|1358->1341|1359->1342|1359->1342|1359->1342|1360->1343|1361->1344|1361->1344|1362->1345|1365->1348|1365->1348|1368->1351|1368->1351|1368->1351|1369->1352|1369->1352|1369->1352|1370->1353|1371->1354|1371->1354|1373->1356|1380->1363|1380->1363|1383->1366|1383->1366|1383->1366|1384->1367|1384->1367|1384->1367|1385->1368|1386->1369|1386->1369|1388->1371|1391->1374|1391->1374|1393->1376|1393->1376|1393->1376|1394->1377|1394->1377|1394->1377|1395->1378|1396->1379|1396->1379|1398->1381|1402->1385|1402->1385|1404->1387|1405->1388|1405->1388|1406->1389|1409->1392|1409->1392|1411->1394|1411->1394|1411->1394|1412->1395|1414->1397|1414->1397|1416->1399|1416->1399|1416->1399|1417->1400|1419->1402|1419->1402|1421->1404|1421->1404|1421->1404|1422->1405|1422->1405|1422->1405|1423->1406|1423->1406|1423->1406|1424->1407|1426->1409|1426->1409|1427->1410|1427->1410|1427->1410|1428->1411|1430->1413|1430->1413|1431->1414|1432->1415|1432->1415|1432->1415|1432->1415|1433->1416|1433->1416|1433->1416|1434->1417|1435->1418|1435->1418|1436->1419|1436->1419|1436->1419|1437->1420|1438->1421|1438->1421|1439->1422|1440->1423|1440->1423|1441->1424|1441->1424|1443->1426|1737->1720|1737->1720|1737->1720|1738->1721|1738->1721|1738->1721|1738->1721|1738->1721|1738->1721|1738->1721|1738->1721|1738->1721|1738->1721|1739->1722|1741->1724|1743->1726|1743->1726
                  -- GENERATED --
              */
          
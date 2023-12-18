
package xml

import _root_.play.twirl.api.JavaScript
import _root_.play.twirl.api.Xml
import _root_.play.twirl.api.Html
import _root_.play.twirl.api.TwirlHelperImports._
import _root_.play.twirl.api.TwirlFeatureImports._
import _root_.play.twirl.api.Txt
/*2.2*/import org.intocps.verification.scenarioverifier.core._

object CosimUppaalTemplate extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.XmlFormat.Appendable,_root_.play.twirl.api.Format[play.twirl.api.XmlFormat.Appendable]](play.twirl.api.XmlFormat) with _root_.play.twirl.api.Template1[ModelEncoding,play.twirl.api.XmlFormat.Appendable] {

  /**/
  def apply/*3.10*/(m: ModelEncoding):play.twirl.api.XmlFormat.Appendable = {
    _display_ {
      {


Seq[Any](format.raw/*4.1*/("""        """),format.raw/*4.9*/("""<?xml version="1.0" encoding="utf-8"?>
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
        bool shouldChecksBeDisabled()"""),format.raw/*38.38*/("""{"""),format.raw/*38.39*/("""
            """),format.raw/*39.13*/("""//In case a loop is not activated all checks should be
            if(loopActive == -1 &amp;&amp; !stepFinderActive)"""),format.raw/*40.62*/("""{"""),format.raw/*40.63*/("""
                """),format.raw/*41.17*/("""return false;
            """),format.raw/*42.13*/("""}"""),format.raw/*42.14*/("""

            """),format.raw/*44.13*/("""//We are inside a loop is it nested
            if(isLoopNested || isStepNested)"""),format.raw/*45.45*/("""{"""),format.raw/*45.46*/("""
                """),format.raw/*46.17*/("""//Both loops should be on the extraIteration
                return !(isStepExtraIteration &amp;&amp; isLoopExtraIteration);
            """),format.raw/*48.13*/("""}"""),format.raw/*48.14*/("""

            """),format.raw/*50.13*/("""//Not nested - if none of the loops is in the extra iteration we should disable the checks
            if(!isLoopExtraIteration &amp;&amp; !isStepExtraIteration)"""),format.raw/*51.71*/("""{"""),format.raw/*51.72*/("""
                """),format.raw/*52.17*/("""return true;
            """),format.raw/*53.13*/("""}"""),format.raw/*53.14*/("""

            """),format.raw/*55.13*/("""return false;
        """),format.raw/*56.9*/("""}"""),format.raw/*56.10*/("""

        """),format.raw/*58.9*/("""//FMU of a variable
        const int undefined := 0;
        const int defined := 1;
        const int notStable :=-1;

        //FMU of the variable
        typedef struct """),format.raw/*64.24*/("""{"""),format.raw/*64.25*/("""
            """),format.raw/*65.13*/("""int[-1,1] status;
            int time;
        """),format.raw/*67.9*/("""}"""),format.raw/*67.10*/(""" """),format.raw/*67.11*/("""variable;


        //Const assignment types - to future variables or current:
        const int final := 0;
        const int tentative := 1;
        const int noCommitment := -1;

        //***********************************************************************************************************

        //Max number of inputs/outputs any FMU can have - Should be changed
        const int MaxNInputs = """),_display_(/*78.33*/m/*78.34*/.maxNInputs),format.raw/*78.45*/(""";
        const int MaxNOutputs = """),_display_(/*79.34*/m/*79.35*/.maxNOutputs),format.raw/*79.47*/(""";

        //Numbers of FMUs in scenario - Should be changed
        const int nFMU = """),_display_(/*82.27*/m/*82.28*/.nFMUs),format.raw/*82.34*/(""";

        //number of algebraic loops in scenario - Should be changed
        const int nAlgebraicLoopsInInit := """),_display_(/*85.45*/m/*85.46*/.nAlgebraicLoopsInInit),format.raw/*85.68*/(""";
        const int nAlgebraicLoopsInStep := """),_display_(/*86.45*/m/*86.46*/.nAlgebraicLoopsInStep),format.raw/*86.68*/(""";

        //Adaptive co-simulation - numbers of different configurations
        const int nConfig := """),_display_(/*89.31*/m/*89.32*/.nConfigs),format.raw/*89.41*/(""";
        //***********************************************************************************************************
        //Do not change

        const int NActions := 14;

        //The number of actions in our system
        const int N := MaxNInputs &gt; MaxNOutputs? MaxNInputs : MaxNOutputs;

        //The maximum step allowed in system - shouldn't be changed
        const int H_max := """),_display_(/*99.29*/m/*99.30*/.Hmax),format.raw/*99.35*/(""";
        const int H := H_max;

        const int noStep := -1;
        const int noFMU := -1;
        const int noLoop := -1;

        typedef struct """),format.raw/*106.24*/("""{"""),format.raw/*106.25*/("""
            """),format.raw/*107.13*/("""int[-1, nFMU] FMU;
            int[-1,NActions] act;
            int[-1,N] portVariable;
            int[-1,H] step_size;
            int[-1,nFMU] relative_step_size;
            int[-1,1] commitment;
            int[-1, nAlgebraicLoopsInStep] loop;
        """),format.raw/*114.9*/("""}"""),format.raw/*114.10*/(""" """),format.raw/*114.11*/("""Operation;

        typedef struct """),format.raw/*116.24*/("""{"""),format.raw/*116.25*/("""
            """),format.raw/*117.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNInputs] input;
            int[-1, MaxNOutputs] output;
        """),format.raw/*120.9*/("""}"""),format.raw/*120.10*/(""" """),format.raw/*120.11*/("""InternalConnection;

        //Types of input ports
        const int delayed := 0;
        const int reactive := 1;
        const int noPort := -1;

        typedef struct """),format.raw/*127.24*/("""{"""),format.raw/*127.25*/("""
            """),format.raw/*128.13*/("""int[0, nFMU] SrcFMU;
            int[0,MaxNOutputs] output;
            int[0,nFMU] TrgFMU;
            int[0,MaxNInputs] input;
        """),format.raw/*132.9*/("""}"""),format.raw/*132.10*/(""" """),format.raw/*132.11*/("""ExternalConnection;

        typedef struct """),format.raw/*134.24*/("""{"""),format.raw/*134.25*/("""
            """),format.raw/*135.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNOutputs] port;
        """),format.raw/*137.9*/("""}"""),format.raw/*137.10*/(""" """),format.raw/*137.11*/("""FmuOutputPort;


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
        const int nInternal := """),_display_(/*200.33*/m/*200.34*/.nInternal),format.raw/*200.44*/(""";
        const int nInternalInit := """),_display_(/*201.37*/m/*201.38*/.nInternalInit),format.raw/*201.52*/(""";

        //Number of external connections in scenario
        const int nExternal := """),_display_(/*204.33*/m/*204.34*/.nExternal),format.raw/*204.44*/(""";

        //The initial of value of h
        int h := H_max;

        //This array is representing the variables of the stepSize that each FMU can take - H_max is the default value
        int stepVariables[nFMU] = """),format.raw/*210.35*/("""{"""),_display_(/*210.37*/m/*210.38*/.stepVariables),format.raw/*210.52*/("""}"""),format.raw/*210.53*/(""";

        //A generic action to pick the next action
        void unpackOperation(Operation operation)"""),format.raw/*213.50*/("""{"""),format.raw/*213.51*/("""
            """),format.raw/*214.13*/("""//action to be performed
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
            if(loopActive == noLoop)"""),format.raw/*226.37*/("""{"""),format.raw/*226.38*/("""
                """),format.raw/*227.17*/("""loopActive := operation.loop;
            """),format.raw/*228.13*/("""}"""),format.raw/*228.14*/("""
            """),format.raw/*229.13*/("""if(action == step)"""),format.raw/*229.31*/("""{"""),format.raw/*229.32*/("""
                """),format.raw/*230.17*/("""if (stepsize == noStep) """),format.raw/*230.41*/("""{"""),format.raw/*230.42*/("""
                    """),format.raw/*231.21*/("""// Step is relative to the fmu referred to by relative_step_size
                    stepsize := stepVariables[relative_step_size];
                """),format.raw/*233.17*/("""}"""),format.raw/*233.18*/(""" """),format.raw/*233.19*/("""else if (stepsize == H) """),format.raw/*233.43*/("""{"""),format.raw/*233.44*/("""
                    """),format.raw/*234.21*/("""// Default step
                    stepsize := h;
                """),format.raw/*236.17*/("""}"""),format.raw/*236.18*/(""" """),format.raw/*236.19*/("""else """),format.raw/*236.24*/("""{"""),format.raw/*236.25*/("""
                    """),format.raw/*237.21*/("""// Absolute step size
                    // Nothing to do.
                """),format.raw/*239.17*/("""}"""),format.raw/*239.18*/("""
            """),format.raw/*240.13*/("""}"""),format.raw/*240.14*/("""
            """),format.raw/*241.13*/("""//Update checkStatus
            checksDisabled = shouldChecksBeDisabled();
        """),format.raw/*243.9*/("""}"""),format.raw/*243.10*/("""


        """),format.raw/*246.9*/("""//Encoding of the scenario
        //Each FMU should have a different ID \in [0, nFMU-1]
        """),_display_(/*248.10*/for(fName<- m.fmuNames) yield /*248.33*/ {_display_(Seq[Any](format.raw/*248.35*/("""
        """),format.raw/*249.9*/("""const int """),_display_(/*249.20*/fName),format.raw/*249.25*/(""" """),format.raw/*249.26*/(""":= """),_display_(/*249.30*/m/*249.31*/.fmuId(fName)),format.raw/*249.44*/(""";
        """)))}),format.raw/*250.10*/("""

        """),format.raw/*252.9*/("""//Number of inputs and outputs of each FMU
        """),_display_(/*253.10*/for(fName<- m.fmuNames) yield /*253.33*/ {_display_(Seq[Any](format.raw/*253.35*/("""
        """),format.raw/*254.9*/("""const int """),_display_(/*254.20*/{fName}),format.raw/*254.27*/("""_input := """),_display_(/*254.38*/m/*254.39*/.nInputs(fName)),format.raw/*254.54*/(""";
        const int """),_display_(/*255.20*/{fName}),format.raw/*255.27*/("""_output := """),_display_(/*255.39*/m/*255.40*/.nOutputs(fName)),format.raw/*255.56*/(""";
        """)))}),format.raw/*256.10*/("""

        """),format.raw/*258.9*/("""//Definition of inputs and outputs of each FMU
        """),_display_(/*259.10*/for(fName<- m.fmuNames) yield /*259.33*/ {_display_(Seq[Any](format.raw/*259.35*/("""
        """),format.raw/*260.9*/("""// """),_display_(/*260.13*/fName),format.raw/*260.18*/(""" """),format.raw/*260.19*/("""inputs - """),_display_(/*260.29*/m/*260.30*/.nInputs(fName)),format.raw/*260.45*/("""
        """),_display_(/*261.10*/for(inName<- m.fmuInNames(fName)) yield /*261.43*/ {_display_(Seq[Any](format.raw/*261.45*/("""
        """),format.raw/*262.9*/("""const int """),_display_(/*262.20*/{m.fmuPortName(fName, inName)}),format.raw/*262.50*/(""" """),format.raw/*262.51*/(""":= """),_display_(/*262.55*/m/*262.56*/.fmuInputEncoding(fName)/*262.80*/(inName)),format.raw/*262.88*/(""";
        """)))}),format.raw/*263.10*/("""
        """),format.raw/*264.9*/("""// """),_display_(/*264.13*/fName),format.raw/*264.18*/(""" """),format.raw/*264.19*/("""outputs - """),_display_(/*264.30*/m/*264.31*/.nOutputs(fName)),format.raw/*264.47*/("""
        """),_display_(/*265.10*/for(outName<- m.fmuOutNames(fName)) yield /*265.45*/ {_display_(Seq[Any](format.raw/*265.47*/("""
        """),format.raw/*266.9*/("""const int """),_display_(/*266.20*/{m.fmuPortName(fName, outName)}),format.raw/*266.51*/(""" """),format.raw/*266.52*/(""":= """),_display_(/*266.56*/m/*266.57*/.fmuOutputEncoding(fName)/*266.82*/(outName)),format.raw/*266.91*/(""";
        """)))}),format.raw/*267.10*/("""
        """),format.raw/*268.9*/("""const int """),_display_(/*268.20*/{fName}),format.raw/*268.27*/("""_inputTypes[nConfig][MaxNInputs] := """),format.raw/*268.63*/("""{"""),format.raw/*268.64*/(""" """),_display_(/*268.66*/m/*268.67*/.fmuInputTypes(fName)),format.raw/*268.88*/(""" """),format.raw/*268.89*/("""}"""),format.raw/*268.90*/(""";
        """)))}),format.raw/*269.10*/("""

        """),format.raw/*271.9*/("""//This array is to keep track of the value of each output port - each output port needs two variables (current and future)
        // and each variable is having two values (defined and time)
        variable connectionVariable[nFMU][MaxNOutputs][2] = """),format.raw/*273.61*/("""{"""),format.raw/*273.62*/(""" """),_display_(/*273.64*/m/*273.65*/.connectionVariable),format.raw/*273.84*/(""" """),format.raw/*273.85*/("""}"""),format.raw/*273.86*/(""";

        //Connections - do not longer contain the type of the input - but it is still a 1:1 mapping
        const ExternalConnection external[nConfig][nExternal] = """),format.raw/*276.65*/("""{"""),format.raw/*276.66*/(""" """),_display_(/*276.68*/m/*276.69*/.external),format.raw/*276.78*/(""" """),format.raw/*276.79*/("""}"""),format.raw/*276.80*/(""";

        const InternalConnection feedthroughInStep[nConfig][nInternal] = """),format.raw/*278.74*/("""{"""),format.raw/*278.75*/(""" """),_display_(/*278.77*/m/*278.78*/.feedthroughInStep),format.raw/*278.96*/(""" """),format.raw/*278.97*/("""}"""),format.raw/*278.98*/(""";

        //The initial internal connection could be different from the connection in the simulation and should be represented differently
        const InternalConnection feedthroughInInit[nInternalInit] = """),format.raw/*281.69*/("""{"""),format.raw/*281.70*/(""" """),_display_(/*281.72*/m/*281.73*/.feedthroughInInit),format.raw/*281.91*/(""" """),format.raw/*281.92*/("""}"""),format.raw/*281.93*/(""";

        //The array show if an FMU can reject a step or not - if the FMU can reject a step the value is 1 on the index defined by the fmus
        const bool mayRejectStep[nFMU] = """),format.raw/*284.42*/("""{"""),format.raw/*284.43*/(""" """),_display_(/*284.45*/m/*284.46*/.mayRejectStep),format.raw/*284.60*/(""" """),format.raw/*284.61*/("""}"""),format.raw/*284.62*/(""";


        const int maxStepOperations := """),_display_(/*287.41*/m/*287.42*/.maxStepOperations),format.raw/*287.60*/(""";

        //Numbers of operations in each step
        const int nInstantiationOperations := """),_display_(/*290.48*/m/*290.49*/.nInstantiationOperations),format.raw/*290.74*/(""";
        const int nInitializationOperations := """),_display_(/*291.49*/m/*291.50*/.nInitializationOperations),format.raw/*291.76*/(""";
        const int[0,maxStepOperations] nStepOperations[nConfig] := """),format.raw/*292.68*/("""{"""),_display_(/*292.70*/m/*292.71*/.nStepOperations),format.raw/*292.87*/("""}"""),format.raw/*292.88*/(""";
        const int nTerminationOperations := """),_display_(/*293.46*/m/*293.47*/.nTerminationOperations),format.raw/*293.70*/(""";

        // Numbers for algebraic loop operations in init
        const int maxNAlgebraicLoopOperationsInInit := """),_display_(/*296.57*/m/*296.58*/.maxNAlgebraicLoopOperationsInInit),format.raw/*296.92*/(""";
        const int maxNConvergeOperationsForAlgebraicLoopsInInit := """),_display_(/*297.69*/m/*297.70*/.maxNConvergeOperationsForAlgebraicLoopsInInit),format.raw/*297.116*/(""";

        //Numbers of operations to be performed per algebraic loop in init
        const int[0,maxNConvergeOperationsForAlgebraicLoopsInInit] nConvergencePortsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit] = """),format.raw/*300.133*/("""{"""),_display_(/*300.135*/m/*300.136*/.nConvergencePortsPerAlgebraicLoopInInit),format.raw/*300.176*/("""}"""),format.raw/*300.177*/(""";
        const int[0,maxNAlgebraicLoopOperationsInInit] nOperationsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit] = """),format.raw/*301.115*/("""{"""),_display_(/*301.117*/m/*301.118*/.nOperationsPerAlgebraicLoopInInit),format.raw/*301.152*/("""}"""),format.raw/*301.153*/(""";



        // Number of operations in the step finding loop
        const int maxFindStepOperations := """),_display_(/*306.45*/m/*306.46*/.maxFindStepOperations),format.raw/*306.68*/(""";
        const int maxFindStepRestoreOperations := """),_display_(/*307.52*/m/*307.53*/.maxFindStepRestoreOperations),format.raw/*307.82*/(""";

        const int[0,maxFindStepOperations] nFindStepOperations[nConfig] := """),format.raw/*309.76*/("""{"""),_display_(/*309.78*/m/*309.79*/.nFindStepOperations),format.raw/*309.99*/("""}"""),format.raw/*309.100*/(""";
        const int[0,maxFindStepRestoreOperations] nRestore[nConfig] := """),format.raw/*310.72*/("""{"""),_display_(/*310.74*/m/*310.75*/.nRestore),format.raw/*310.84*/("""}"""),format.raw/*310.85*/(""";

        // Numbers for algebraic loop operations in step
        const int maxNAlgebraicLoopOperationsInStep := """),_display_(/*313.57*/m/*313.58*/.maxNAlgebraicLoopOperationsInStep),format.raw/*313.92*/(""";
        const int maxNRetryOperationsForAlgebraicLoopsInStep := """),_display_(/*314.66*/m/*314.67*/.maxNRetryOperationsForAlgebraicLoopsInStep),format.raw/*314.110*/(""";
        const int maxNConvergeOperationsForAlgebraicLoopsInStep := """),_display_(/*315.69*/m/*315.70*/.maxNConvergeOperationsForAlgebraicLoopsInStep),format.raw/*315.116*/(""";

        //Numbers of operations to be performed per algebraic loop in step
        const int[0,maxNConvergeOperationsForAlgebraicLoopsInStep] nConvergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*318.142*/("""{"""),_display_(/*318.144*/m/*318.145*/.nConvergencePortsPerAlgebraicLoopInStep),format.raw/*318.185*/("""}"""),format.raw/*318.186*/(""";
        const int[0,maxNAlgebraicLoopOperationsInStep] nOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*319.124*/("""{"""),_display_(/*319.126*/m/*319.127*/.nOperationsPerAlgebraicLoopInStep),format.raw/*319.161*/("""}"""),format.raw/*319.162*/(""";
        const int[0,maxNRetryOperationsForAlgebraicLoopsInStep] nRetryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*320.138*/("""{"""),_display_(/*320.140*/m/*320.141*/.nRetryOperationsPerAlgebraicLoopInStep),format.raw/*320.180*/("""}"""),format.raw/*320.181*/(""";

        //These operations define what should be performed in the simulation - it is assumed that the operation first loads the fmus
        const Operation instantiationOperations[nInstantiationOperations] = """),format.raw/*323.77*/("""{"""),format.raw/*323.78*/(""" """),_display_(/*323.80*/m/*323.81*/.instantiationOperations),format.raw/*323.105*/(""" """),format.raw/*323.106*/("""}"""),format.raw/*323.107*/(""";

        const Operation initializationOperations[nInitializationOperations] = """),format.raw/*325.79*/("""{"""),format.raw/*325.80*/(""" """),_display_(/*325.82*/m/*325.83*/.initializationOperations),format.raw/*325.108*/(""" """),format.raw/*325.109*/("""}"""),format.raw/*325.110*/(""";

        const Operation stepOperations[nConfig][maxStepOperations] = """),format.raw/*327.70*/("""{"""),format.raw/*327.71*/(""" """),_display_(/*327.73*/m/*327.74*/.stepOperations),format.raw/*327.89*/(""" """),format.raw/*327.90*/("""}"""),format.raw/*327.91*/(""";

        //These are the operations to be performed in order to find the correct step
        //In these operation there is a difference on the third parameter to doStep:
        // H (A step-value greater than the allowed step (Greater than the number of FMUS)) means that we should look at the variable h
        // A stepSize (0:(nFMU-1)) means that the should look at that index in stepVariables use that as the step
        //This is being done inside - findStepAction

        const Operation findStepIteration[nConfig][maxFindStepOperations] = """),format.raw/*335.77*/("""{"""),format.raw/*335.78*/(""" """),_display_(/*335.80*/m/*335.81*/.findStepLoopOperations),format.raw/*335.104*/(""" """),format.raw/*335.105*/("""}"""),format.raw/*335.106*/(""";
        const Operation StepFix[nConfig][maxFindStepRestoreOperations] = """),format.raw/*336.74*/("""{"""),format.raw/*336.75*/(""" """),_display_(/*336.77*/m/*336.78*/.findStepLoopRestoreOperations),format.raw/*336.108*/(""" """),format.raw/*336.109*/("""}"""),format.raw/*336.110*/(""";

        //Possible multiple loops
        //Loop operations are to solve algebraic loops in the co-simulation scenario
        const Operation operationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNAlgebraicLoopOperationsInStep] = """),format.raw/*340.127*/("""{"""),_display_(/*340.129*/m/*340.130*/.operationsPerAlgebraicLoopInStep),format.raw/*340.163*/(""" """),format.raw/*340.164*/("""}"""),format.raw/*340.165*/(""";
        const Operation operationsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit][maxNAlgebraicLoopOperationsInInit] = """),format.raw/*341.118*/("""{"""),_display_(/*341.120*/m/*341.121*/.operationsPerAlgebraicLoopInInit),format.raw/*341.154*/(""" """),format.raw/*341.155*/("""}"""),format.raw/*341.156*/(""";

        //The converge ports is to mark which variables that needs to be checked in the convergence loop
        //The convention is now to specify the FMU first and the port to denote the variables that should be checked
        const FmuOutputPort convergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNConvergeOperationsForAlgebraicLoopsInStep] = """),format.raw/*345.149*/("""{"""),_display_(/*345.151*/m/*345.152*/.convergencePortsPerAlgebraicLoopInStep),format.raw/*345.191*/(""" """),format.raw/*345.192*/("""}"""),format.raw/*345.193*/(""";
        const FmuOutputPort convergencePortsPerAlgebraicLoopInInit[nAlgebraicLoopsInInit][maxNConvergeOperationsForAlgebraicLoopsInInit] = """),format.raw/*346.140*/("""{"""),_display_(/*346.142*/m/*346.143*/.convergencePortsPerAlgebraicLoopInInit),format.raw/*346.182*/(""" """),format.raw/*346.183*/("""}"""),format.raw/*346.184*/(""";

        const Operation retryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNRetryOperationsForAlgebraicLoopsInStep] = """),format.raw/*348.141*/("""{"""),_display_(/*348.143*/m/*348.144*/.retryOperationsPerAlgebraicLoopInStep),format.raw/*348.182*/(""" """),format.raw/*348.183*/("""}"""),format.raw/*348.184*/(""";

        const Operation terminationOperations[nTerminationOperations] = """),format.raw/*350.73*/("""{"""),format.raw/*350.74*/(""" """),_display_(/*350.76*/m/*350.77*/.terminationOperations),format.raw/*350.99*/(""" """),format.raw/*350.100*/("""}"""),format.raw/*350.101*/(""";

    </declaration>
<template>
<name>Interpreter</name>
<declaration>
    int inst_pc := 0;
    int init_pc := 0;
    int cosimstep_pc := 0;
    int terminate_pc := 0;
    int n := 0;

    void selectNextInstAction()"""),format.raw/*362.32*/("""{"""),format.raw/*362.33*/("""
        """),format.raw/*363.9*/("""unpackOperation(instantiationOperations[inst_pc]);
        //Proceed to next action
        inst_pc++;
    """),format.raw/*366.5*/("""}"""),format.raw/*366.6*/("""

    """),format.raw/*368.5*/("""void selectNextInitAction()"""),format.raw/*368.32*/("""{"""),format.raw/*368.33*/("""
        """),format.raw/*369.9*/("""unpackOperation(initializationOperations[init_pc]);
        //Proceed to next action
        init_pc++;
    """),format.raw/*372.5*/("""}"""),format.raw/*372.6*/("""


    """),format.raw/*375.5*/("""void selectNextCosimStepAction()"""),format.raw/*375.37*/("""{"""),format.raw/*375.38*/("""
        """),format.raw/*376.9*/("""if(cosimstep_pc &lt; nStepOperations[currentConfig])"""),format.raw/*376.61*/("""{"""),format.raw/*376.62*/("""
            """),format.raw/*377.13*/("""unpackOperation(stepOperations[currentConfig][cosimstep_pc]);
        """),format.raw/*378.9*/("""}"""),format.raw/*378.10*/("""
        """),format.raw/*379.9*/("""//Proceed to next action
        cosimstep_pc++;
    """),format.raw/*381.5*/("""}"""),format.raw/*381.6*/("""

    """),format.raw/*383.5*/("""void findFMUTerminateAction()"""),format.raw/*383.34*/("""{"""),format.raw/*383.35*/("""
        """),format.raw/*384.9*/("""unpackOperation(terminationOperations[terminate_pc]);
        //Proceed to next action
        terminate_pc++;
    """),format.raw/*387.5*/("""}"""),format.raw/*387.6*/("""


    """),format.raw/*390.5*/("""void takeStep(int global_h, int newConfig)"""),format.raw/*390.47*/("""{"""),format.raw/*390.48*/("""
        """),format.raw/*391.9*/("""//h is progression of time
        time := time + h;
        //Reset the loop actions
        cosimstep_pc := 0;
        //reset the global stepsize
        h := global_h;
        //reset n
        n := 0;
        currentConfig := newConfig;
    """),format.raw/*400.5*/("""}"""),format.raw/*400.6*/("""

    """),format.raw/*402.5*/("""void setStepsizeFMU(int fmu, int fmu_step_size)"""),format.raw/*402.52*/("""{"""),format.raw/*402.53*/("""
        """),format.raw/*403.9*/("""if(mayRejectStep[fmu])"""),format.raw/*403.31*/("""{"""),format.raw/*403.32*/("""
            """),format.raw/*404.13*/("""//If an FMU can reject a Step it is maximum step should be updated in each iteration
            stepVariables[fmu] = fmu_step_size;
        """),format.raw/*406.9*/("""}"""),format.raw/*406.10*/("""else"""),format.raw/*406.14*/("""{"""),format.raw/*406.15*/("""
            """),format.raw/*407.13*/("""//If not just set its maximum step to the global step
            stepVariables[fmu] = h;
        """),format.raw/*409.9*/("""}"""),format.raw/*409.10*/("""
        """),format.raw/*410.9*/("""n++;
    """),format.raw/*411.5*/("""}"""),format.raw/*411.6*/("""
"""),format.raw/*412.1*/("""</declaration>
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

    void selectNextLoopAction(int l)"""),format.raw/*675.37*/("""{"""),format.raw/*675.38*/("""
        """),format.raw/*676.9*/("""unpackOperation(operationsPerAlgebraicLoopInInit[l][convergence_pc]);
        //Proceed to next action
        convergence_pc ++;
    """),format.raw/*679.5*/("""}"""),format.raw/*679.6*/("""

    """),format.raw/*681.5*/("""void updateConvergenceVariables(int l)"""),format.raw/*681.43*/("""{"""),format.raw/*681.44*/("""
        """),format.raw/*682.9*/("""int fmu;
        int v;
        for(i = 0; i &lt; nConvergencePortsPerAlgebraicLoopInInit[l]; i++)"""),format.raw/*684.75*/("""{"""),format.raw/*684.76*/("""
            """),format.raw/*685.13*/("""fmu = convergencePortsPerAlgebraicLoopInInit[l][i].FMU;
            v = convergencePortsPerAlgebraicLoopInInit[l][i].port;
            connectionVariable[fmu][v][tentative].status = connectionVariable[fmu][v][final].status;
            connectionVariable[fmu][v][tentative].time = connectionVariable[fmu][v][final].time;
        """),format.raw/*689.9*/("""}"""),format.raw/*689.10*/("""
    """),format.raw/*690.5*/("""}"""),format.raw/*690.6*/("""

    """),format.raw/*692.5*/("""void loopConverge()"""),format.raw/*692.24*/("""{"""),format.raw/*692.25*/("""
        """),format.raw/*693.9*/("""//Loop not longer active
        loopActive := -1;
        //Loop action counter reset
        convergence_pc := 0;
        //Reset convergence counter
        currentIteration := 0;
    """),format.raw/*699.5*/("""}"""),format.raw/*699.6*/("""


    """),format.raw/*702.5*/("""void resetConvergenceloop()"""),format.raw/*702.32*/("""{"""),format.raw/*702.33*/("""
        """),format.raw/*703.9*/("""convergence_pc := 0;
        selectNextLoopAction(loopActive);
    """),format.raw/*705.5*/("""}"""),format.raw/*705.6*/("""

    """),format.raw/*707.5*/("""//Convergence will happen when all convergenceVariables have a similar future and current value
    bool convergenceCriteria(int l)"""),format.raw/*708.36*/("""{"""),format.raw/*708.37*/("""
        """),format.raw/*709.9*/("""return forall(x:int[0,maxNConvergeOperationsForAlgebraicLoopsInInit-1])
            convergencePortsPerAlgebraicLoopInInit[l][x].FMU != noFMU imply connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][final].status
            == connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][tentative].status
            &amp;&amp;
            connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][final].time
            == connectionVariable[convergencePortsPerAlgebraicLoopInInit[l][x].FMU][convergencePortsPerAlgebraicLoopInInit[l][x].port][tentative].time;
    """),format.raw/*715.5*/("""}"""),format.raw/*715.6*/("""

    """),format.raw/*717.5*/("""bool convergence(int l)"""),format.raw/*717.28*/("""{"""),format.raw/*717.29*/("""
        """),format.raw/*718.9*/("""return (convergenceCriteria(l) &amp;&amp; isLoopExtraIteration);
    """),format.raw/*719.5*/("""}"""),format.raw/*719.6*/("""


    """),format.raw/*722.5*/("""void updateIsExtra(int l)"""),format.raw/*722.30*/("""{"""),format.raw/*722.31*/("""
        """),format.raw/*723.9*/("""if(convergenceCriteria(l))"""),format.raw/*723.35*/("""{"""),format.raw/*723.36*/("""
            """),format.raw/*724.13*/("""isLoopExtraIteration := true;
        """),format.raw/*725.9*/("""}"""),format.raw/*725.10*/("""
    """),format.raw/*726.5*/("""}"""),format.raw/*726.6*/("""
"""),format.raw/*727.1*/("""</declaration>
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



    void selectNextLoopAction(int l)"""),format.raw/*834.37*/("""{"""),format.raw/*834.38*/("""
        """),format.raw/*835.9*/("""unpackOperation(operationsPerAlgebraicLoopInStep[currentConfig][l][convergence_pc]);
        //Proceed to next action
        convergence_pc ++;
    """),format.raw/*838.5*/("""}"""),format.raw/*838.6*/("""

    """),format.raw/*840.5*/("""void selectNextRestoreAction(int l)"""),format.raw/*840.40*/("""{"""),format.raw/*840.41*/("""
        """),format.raw/*841.9*/("""unpackOperation(retryOperationsPerAlgebraicLoopInStep[currentConfig][l][restore_pc]);
        restore_pc++;
    """),format.raw/*843.5*/("""}"""),format.raw/*843.6*/("""


    """),format.raw/*846.5*/("""void updateConvergenceVariables(int l)"""),format.raw/*846.43*/("""{"""),format.raw/*846.44*/("""
        """),format.raw/*847.9*/("""int fmu;
        int v;
        int i = 0;
        for(i = 0; i &lt; nConvergencePortsPerAlgebraicLoopInStep[currentConfig][l]; i++)"""),format.raw/*850.90*/("""{"""),format.raw/*850.91*/("""
            """),format.raw/*851.13*/("""fmu = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].FMU;
            v = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].port;
            if(isFeedthrough)"""),format.raw/*853.30*/("""{"""),format.raw/*853.31*/("""
                """),format.raw/*854.17*/("""connectionVariable[fmu][v][tentative].status := connectionVariable[fmu][v][final].status;
                connectionVariable[fmu][v][tentative].time := connectionVariable[fmu][v][final].time;
            """),format.raw/*856.13*/("""}"""),format.raw/*856.14*/("""else"""),format.raw/*856.18*/("""{"""),format.raw/*856.19*/("""
                """),format.raw/*857.17*/("""connectionVariable[fmu][v][final].status := connectionVariable[fmu][v][tentative].status;
                connectionVariable[fmu][v][final].time := connectionVariable[fmu][v][tentative].time;
            """),format.raw/*859.13*/("""}"""),format.raw/*859.14*/("""
        """),format.raw/*860.9*/("""}"""),format.raw/*860.10*/("""
    """),format.raw/*861.5*/("""}"""),format.raw/*861.6*/("""

    """),format.raw/*863.5*/("""void loopConverge()"""),format.raw/*863.24*/("""{"""),format.raw/*863.25*/("""
        """),format.raw/*864.9*/("""//Loop not longer active
        loopActive := -1;
        //Loop action counter reset
        convergence_pc := 0;
        //Reset convergence counter
        currentConvergeLoopIteration := 0;
        isLoopExtraIteration:= false;
        isFeedthrough := false;
    """),format.raw/*872.5*/("""}"""),format.raw/*872.6*/("""


    """),format.raw/*875.5*/("""void resetConvergenceloop()"""),format.raw/*875.32*/("""{"""),format.raw/*875.33*/("""
        """),format.raw/*876.9*/("""convergence_pc := 0;
        restore_pc := 0;
        selectNextLoopAction(loopActive);
    """),format.raw/*879.5*/("""}"""),format.raw/*879.6*/("""

    """),format.raw/*881.5*/("""//Convergence will happen when all convergenceVariables have a similar future and current value
    bool convergenceCriteria(int l)"""),format.raw/*882.36*/("""{"""),format.raw/*882.37*/("""
        """),format.raw/*883.9*/("""return forall(x:int[0,maxNConvergeOperationsForAlgebraicLoopsInStep-1])
            convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU != noFMU imply
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].status
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].status
            &amp;&amp;
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].time
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].time;
    """),format.raw/*892.5*/("""}"""),format.raw/*892.6*/("""

    """),format.raw/*894.5*/("""bool convergence(int l)"""),format.raw/*894.28*/("""{"""),format.raw/*894.29*/("""
        """),format.raw/*895.9*/("""return (convergenceCriteria(l) &amp;&amp; isLoopExtraIteration);
    """),format.raw/*896.5*/("""}"""),format.raw/*896.6*/("""


    """),format.raw/*899.5*/("""void updateIsExtra(int l)"""),format.raw/*899.30*/("""{"""),format.raw/*899.31*/("""
        """),format.raw/*900.9*/("""if(convergenceCriteria(l))"""),format.raw/*900.35*/("""{"""),format.raw/*900.36*/("""
            """),format.raw/*901.13*/("""isLoopExtraIteration := true;
        """),format.raw/*902.9*/("""}"""),format.raw/*902.10*/("""
    """),format.raw/*903.5*/("""}"""),format.raw/*903.6*/("""
"""),format.raw/*904.1*/("""</declaration>
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

    void selectNextStepFinderAction()"""),format.raw/*1056.38*/("""{"""),format.raw/*1056.39*/("""
        """),format.raw/*1057.9*/("""unpackOperation(findStepIteration[currentConfig][step_pc]);
        step_pc++;
    """),format.raw/*1059.5*/("""}"""),format.raw/*1059.6*/("""

    """),format.raw/*1061.5*/("""void selectNextStepRestoreAction()"""),format.raw/*1061.39*/("""{"""),format.raw/*1061.40*/("""
        """),format.raw/*1062.9*/("""unpackOperation(StepFix[currentConfig][restore_pc]);
        restore_pc++;
    """),format.raw/*1064.5*/("""}"""),format.raw/*1064.6*/("""

    """),format.raw/*1066.5*/("""void findMinStep()"""),format.raw/*1066.23*/("""{"""),format.raw/*1066.24*/("""
        """),format.raw/*1067.9*/("""//Maximum step size allowed
        int min = nFMU;
        int j := 0;
        for(j = 0; j &lt; nFMU; j++)"""),format.raw/*1070.37*/("""{"""),format.raw/*1070.38*/("""
            """),format.raw/*1071.13*/("""if(stepVariables[j] &lt; min)"""),format.raw/*1071.42*/("""{"""),format.raw/*1071.43*/("""
                """),format.raw/*1072.17*/("""min := stepVariables[j];
            """),format.raw/*1073.13*/("""}"""),format.raw/*1073.14*/("""
        """),format.raw/*1074.9*/("""}"""),format.raw/*1074.10*/("""
        """),format.raw/*1075.9*/("""h := min;
    """),format.raw/*1076.5*/("""}"""),format.raw/*1076.6*/("""


    """),format.raw/*1079.5*/("""bool stepFound()"""),format.raw/*1079.21*/("""{"""),format.raw/*1079.22*/("""
        """),format.raw/*1080.9*/("""//All FMU that may reject a step should be able to take the same step - h
        return forall(x:int[0, nFMU-1]) mayRejectStep[x] imply stepVariables[x] == h;
    """),format.raw/*1082.5*/("""}"""),format.raw/*1082.6*/("""

    """),format.raw/*1084.5*/("""bool loopConverged()"""),format.raw/*1084.25*/("""{"""),format.raw/*1084.26*/("""
        """),format.raw/*1085.9*/("""return (stepFound() &amp;&amp; isStepExtraIteration);
    """),format.raw/*1086.5*/("""}"""),format.raw/*1086.6*/("""


    """),format.raw/*1089.5*/("""void updateIsExtra()"""),format.raw/*1089.25*/("""{"""),format.raw/*1089.26*/("""
        """),format.raw/*1090.9*/("""if(stepFound())"""),format.raw/*1090.24*/("""{"""),format.raw/*1090.25*/("""
            """),format.raw/*1091.13*/("""isStepExtraIteration := true;
            //Reset numbers of tries to 0 - This is to avoid problems with the maximum number of tries and not to active the nested checks
            numbersOfTries := 0;
        """),format.raw/*1094.9*/("""}"""),format.raw/*1094.10*/("""
    """),format.raw/*1095.5*/("""}"""),format.raw/*1095.6*/("""
"""),format.raw/*1096.1*/("""</declaration>
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
    variable inputVariables[MaxNInputs] = """),format.raw/*1234.43*/("""{"""),format.raw/*1234.44*/(""" """),_display_(/*1234.46*/{m.variableArray(m.maxNInputs)}),format.raw/*1234.77*/(""" """),format.raw/*1234.78*/("""}"""),format.raw/*1234.79*/(""";
    variable outputVariables[MaxNOutputs] = """),format.raw/*1235.45*/("""{"""),format.raw/*1235.46*/(""" """),_display_(/*1235.48*/{m.variableArray(m.maxNOutputs)}),format.raw/*1235.80*/(""" """),format.raw/*1235.81*/("""}"""),format.raw/*1235.82*/(""";

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
    bool getEnabled[MaxNOutputs] := """),format.raw/*1249.37*/("""{"""),format.raw/*1249.38*/(""" """),_display_(/*1249.40*/m/*1249.41*/.getEnabled),format.raw/*1249.52*/(""" """),format.raw/*1249.53*/("""}"""),format.raw/*1249.54*/(""";
    bool setEnabled[MaxNInputs] := """),format.raw/*1250.36*/("""{"""),format.raw/*1250.37*/(""" """),_display_(/*1250.39*/m/*1250.40*/.setEnabled),format.raw/*1250.51*/(""" """),format.raw/*1250.52*/("""}"""),format.raw/*1250.53*/(""";

    void initialize()"""),format.raw/*1252.22*/("""{"""),format.raw/*1252.23*/("""
        """),format.raw/*1253.9*/("""isInitialized := true;
        //Set all variables to undefined
        for(i = 0; i &lt; nInput; i++)"""),format.raw/*1255.39*/("""{"""),format.raw/*1255.40*/("""
            """),format.raw/*1256.13*/("""inputVariables[i].status := undefined;
            inputVariables[i].time := 0;
        """),format.raw/*1258.9*/("""}"""),format.raw/*1258.10*/("""
        """),format.raw/*1259.9*/("""for(i := 0; i &lt; nOutput; i++)"""),format.raw/*1259.41*/("""{"""),format.raw/*1259.42*/("""
            """),format.raw/*1260.13*/("""outputVariables[i].status := undefined;
            outputVariables[i].time := 0;
        """),format.raw/*1262.9*/("""}"""),format.raw/*1262.10*/("""
    """),format.raw/*1263.5*/("""}"""),format.raw/*1263.6*/("""

    """),format.raw/*1265.5*/("""void getValue(int v, int a)"""),format.raw/*1265.32*/("""{"""),format.raw/*1265.33*/("""
        """),format.raw/*1266.9*/("""outputVariables[v].status := defined;
        outputVariables[v].time := cTime;

        connectionVariable[id][v][a].status := defined;
        connectionVariable[id][v][a].time := cTime;
    """),format.raw/*1271.5*/("""}"""),format.raw/*1271.6*/("""

    """),format.raw/*1273.5*/("""void setValue(int v, int a)"""),format.raw/*1273.32*/("""{"""),format.raw/*1273.33*/("""
        """),format.raw/*1274.9*/("""inputVariables[v].status := defined;
        for(i = 0; i &lt; nExternal; i++)"""),format.raw/*1275.42*/("""{"""),format.raw/*1275.43*/("""
            """),format.raw/*1276.13*/("""if(external[currentConfig][i].TrgFMU == id &amp;&amp; external[currentConfig][i].input == v)"""),format.raw/*1276.105*/("""{"""),format.raw/*1276.106*/("""
                """),format.raw/*1277.17*/("""inputVariables[v].time := connectionVariable[external[currentConfig][i].SrcFMU][external[currentConfig][i].output][a].time;
            """),format.raw/*1278.13*/("""}"""),format.raw/*1278.14*/("""
        """),format.raw/*1279.9*/("""}"""),format.raw/*1279.10*/("""
    """),format.raw/*1280.5*/("""}"""),format.raw/*1280.6*/("""

    """),format.raw/*1282.5*/("""//Proceed in time - we will start by assuming an FMU can't reject a stepsize
    void doStep(int t)"""),format.raw/*1283.23*/("""{"""),format.raw/*1283.24*/("""
        """),format.raw/*1284.9*/("""//Checking of step is valid
        if(t &gt; stepVariables[id])"""),format.raw/*1285.37*/("""{"""),format.raw/*1285.38*/("""
        """),format.raw/*1286.9*/("""//Step is too big and will not be allowed - t is reset too the biggest allowed step
            t := stepVariables[id];
        """),format.raw/*1288.9*/("""}"""),format.raw/*1288.10*/("""

        """),format.raw/*1290.9*/("""//Take step
        cTime := cTime + t;

        isConsistent := true;

        for(i = 0; i &lt; nInput; i++)"""),format.raw/*1295.39*/("""{"""),format.raw/*1295.40*/("""
            """),format.raw/*1296.13*/("""if(inputVariables[i].time != cTime)"""),format.raw/*1296.48*/("""{"""),format.raw/*1296.49*/("""
                """),format.raw/*1297.17*/("""isConsistent := false;
            """),format.raw/*1298.13*/("""}"""),format.raw/*1298.14*/("""
        """),format.raw/*1299.9*/("""}"""),format.raw/*1299.10*/("""

        """),format.raw/*1301.9*/("""//Reset outputs accesssed and advance their timestamp
        for(i = 0; i &lt; nOutput; i++)"""),format.raw/*1302.40*/("""{"""),format.raw/*1302.41*/("""
            """),format.raw/*1303.13*/("""//The inputs of the FMUs are inconsistent (not all are at time cTime) - so the FMUs output valid should be set to NaN
            if(isConsistent)"""),format.raw/*1304.29*/("""{"""),format.raw/*1304.30*/("""
                """),format.raw/*1305.17*/("""outputVariables[i].status := undefined;
                outputVariables[i].time := cTime;
            """),format.raw/*1307.13*/("""}"""),format.raw/*1307.14*/("""else"""),format.raw/*1307.18*/("""{"""),format.raw/*1307.19*/("""
                """),format.raw/*1308.17*/("""outputVariables[i].status := notStable;
                outputVariables[i].time := cTime;
            """),format.raw/*1310.13*/("""}"""),format.raw/*1310.14*/("""
        """),format.raw/*1311.9*/("""}"""),format.raw/*1311.10*/("""

        """),format.raw/*1313.9*/("""isConsistent := true;

        //Update or return the taken step size
        stepVariables[id] := t;
    """),format.raw/*1317.5*/("""}"""),format.raw/*1317.6*/("""

    """),format.raw/*1319.5*/("""void restoreFMU()"""),format.raw/*1319.22*/("""{"""),format.raw/*1319.23*/("""
        """),format.raw/*1320.9*/("""outputVariables := savedOutputVariables;
        inputVariables := savedInputVariables;
        cTime := savedTime;
    """),format.raw/*1323.5*/("""}"""),format.raw/*1323.6*/("""

    """),format.raw/*1325.5*/("""void saveFMU()"""),format.raw/*1325.19*/("""{"""),format.raw/*1325.20*/("""
        """),format.raw/*1326.9*/("""savedOutputVariables := outputVariables;
        savedInputVariables := inputVariables;
        savedTime := cTime;
        isSaved := true;
    """),format.raw/*1330.5*/("""}"""),format.raw/*1330.6*/("""

    """),format.raw/*1332.5*/("""bool preSetInit(int v, int a)"""),format.raw/*1332.34*/("""{"""),format.raw/*1332.35*/("""
        """),format.raw/*1333.9*/("""if(checksDisabled)"""),format.raw/*1333.27*/("""{"""),format.raw/*1333.28*/("""
            """),format.raw/*1334.13*/("""return true;
        """),format.raw/*1335.9*/("""}"""),format.raw/*1335.10*/("""
        """),format.raw/*1336.9*/("""//All outputs connected to the input should be defined - no difference between delay and reactive in init. ConnectionVariables an d ExternalConnections are having the same order
        return forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v imply
            connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined;

    """),format.raw/*1340.5*/("""}"""),format.raw/*1340.6*/("""

    """),format.raw/*1342.5*/("""bool preGetInit(int v)"""),format.raw/*1342.27*/("""{"""),format.raw/*1342.28*/("""
        """),format.raw/*1343.9*/("""if(checksDisabled)"""),format.raw/*1343.27*/("""{"""),format.raw/*1343.28*/("""
            """),format.raw/*1344.13*/("""return true;
        """),format.raw/*1345.9*/("""}"""),format.raw/*1345.10*/("""
        """),format.raw/*1346.9*/("""//The internal time should be equivalent to 0 and all variable connected to this one should be defined
        return forall(x:int[0, nInternalInit-1]) feedthroughInInit[x].FMU == id &amp;&amp; feedthroughInInit[x].output == v
            imply inputVariables[feedthroughInInit[x].input].status == defined;
    """),format.raw/*1349.5*/("""}"""),format.raw/*1349.6*/("""


    """),format.raw/*1352.5*/("""bool preSet(int v, int a)"""),format.raw/*1352.30*/("""{"""),format.raw/*1352.31*/("""
    """),format.raw/*1353.5*/("""if(checksDisabled)"""),format.raw/*1353.23*/("""{"""),format.raw/*1353.24*/("""
    """),format.raw/*1354.5*/("""return true;
    """),format.raw/*1355.5*/("""}"""),format.raw/*1355.6*/("""

    """),format.raw/*1357.5*/("""//If the connection is reactive the connected variable needs to have a greater than the time of the FMU and be defined
    return (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp;
    inputType[currentConfig][v] == reactive imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time &gt; cTime) &amp;&amp;
    (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp; inputType[currentConfig][v] == delayed
    imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time == cTime);
    """),format.raw/*1364.5*/("""}"""),format.raw/*1364.6*/("""


    """),format.raw/*1367.5*/("""bool preGet(int v)"""),format.raw/*1367.23*/("""{"""),format.raw/*1367.24*/("""
        """),format.raw/*1368.9*/("""if(checksDisabled)"""),format.raw/*1368.27*/("""{"""),format.raw/*1368.28*/("""
            """),format.raw/*1369.13*/("""return true;
        """),format.raw/*1370.9*/("""}"""),format.raw/*1370.10*/("""

        """),format.raw/*1372.9*/("""//All internal connections should be defined at time cTime
        return forall(x:int[0, nInternal-1]) feedthroughInStep[currentConfig][x].FMU == id &amp;&amp; feedthroughInStep[currentConfig][x].output == v
            imply inputVariables[feedthroughInStep[currentConfig][x].input].status == defined &amp;&amp; inputVariables[feedthroughInStep[currentConfig][x].input].time == cTime;
    """),format.raw/*1375.5*/("""}"""),format.raw/*1375.6*/("""

    """),format.raw/*1377.5*/("""bool preDoStep(int t)"""),format.raw/*1377.26*/("""{"""),format.raw/*1377.27*/("""
        """),format.raw/*1378.9*/("""if(checksDisabled)"""),format.raw/*1378.27*/("""{"""),format.raw/*1378.28*/("""
            """),format.raw/*1379.13*/("""return true;
        """),format.raw/*1380.9*/("""}"""),format.raw/*1380.10*/("""

        """),format.raw/*1382.9*/("""//All delayed input ports should be defined at the current time
        //And all reactive inputs ports should be defined at the next time step
        return (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == reactive imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime + t) &amp;&amp;
            (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == delayed imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime);
    """),format.raw/*1386.5*/("""}"""),format.raw/*1386.6*/("""

        """),format.raw/*1388.9*/("""//An FMU can only enter the Simulation mode when all connected FMU variables are defined at time 0
    bool preSimulation()"""),format.raw/*1389.25*/("""{"""),format.raw/*1389.26*/("""
        """),format.raw/*1390.9*/("""return ((forall(x:int[0, MaxNOutputs-1]) outputVariables[x].status == defined &amp;&amp; outputVariables[x].time == 0)
        &amp;&amp; (forall(x:int[0, MaxNInputs-1]) inputVariables[x].status == defined &amp;&amp;
        inputVariables[x].time == 0));
    """),format.raw/*1393.5*/("""}"""),format.raw/*1393.6*/("""

    """),format.raw/*1395.5*/("""bool preSaveFMU()"""),format.raw/*1395.22*/("""{"""),format.raw/*1395.23*/("""
        """),format.raw/*1396.9*/("""//Always possible
        return true;
    """),format.raw/*1398.5*/("""}"""),format.raw/*1398.6*/("""

    """),format.raw/*1400.5*/("""bool preRestoreFMU()"""),format.raw/*1400.25*/("""{"""),format.raw/*1400.26*/("""
        """),format.raw/*1401.9*/("""//Should a requirement be a saved previous FMU?
        return isSaved;
    """),format.raw/*1403.5*/("""}"""),format.raw/*1403.6*/("""

    """),format.raw/*1405.5*/("""void updateEnableActions()"""),format.raw/*1405.31*/("""{"""),format.raw/*1405.32*/("""
        """),format.raw/*1406.9*/("""if(!isInitialized)"""),format.raw/*1406.27*/("""{"""),format.raw/*1406.28*/("""
            """),format.raw/*1407.13*/("""for(i = 0; i &lt; nInput; i++)"""),format.raw/*1407.43*/("""{"""),format.raw/*1407.44*/("""
                """),format.raw/*1408.17*/("""setEnabled[i] := preSetInit(i, final) &amp;&amp;
                inputVariables[i].status == undefined;
            """),format.raw/*1410.13*/("""}"""),format.raw/*1410.14*/("""
            """),format.raw/*1411.13*/("""for(i := 0; i &lt; nOutput; i++)"""),format.raw/*1411.45*/("""{"""),format.raw/*1411.46*/("""
                """),format.raw/*1412.17*/("""getEnabled[i] := preGetInit(i) &amp;&amp;
                outputVariables[i].status == undefined;
            """),format.raw/*1414.13*/("""}"""),format.raw/*1414.14*/("""
            """),format.raw/*1415.13*/("""stepEnabled := false;
        """),format.raw/*1416.9*/("""}"""),format.raw/*1416.10*/("""else"""),format.raw/*1416.14*/("""{"""),format.raw/*1416.15*/("""
            """),format.raw/*1417.13*/("""for(i = 0; i &lt; nInput; i++)"""),format.raw/*1417.43*/("""{"""),format.raw/*1417.44*/("""
                """),format.raw/*1418.17*/("""setEnabled[i] := preSet(i, final);
            """),format.raw/*1419.13*/("""}"""),format.raw/*1419.14*/("""
            """),format.raw/*1420.13*/("""for(i := 0; i &lt; nOutput; i++)"""),format.raw/*1420.45*/("""{"""),format.raw/*1420.46*/("""
                """),format.raw/*1421.17*/("""getEnabled[i] := preGet(i);
            """),format.raw/*1422.13*/("""}"""),format.raw/*1422.14*/("""
            """),format.raw/*1423.13*/("""stepEnabled := preDoStep(h);
        """),format.raw/*1424.9*/("""}"""),format.raw/*1424.10*/("""
    """),format.raw/*1425.5*/("""}"""),format.raw/*1425.6*/("""

"""),format.raw/*1427.1*/("""</declaration>
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
        """),_display_(/*1721.10*/for(fName<- m.fmuNames) yield /*1721.33*/ {_display_(Seq[Any](format.raw/*1721.35*/("""
        """),_display_(/*1722.10*/{fName}),format.raw/*1722.17*/("""_fmu = FMU("""),_display_(/*1722.29*/{fName}),format.raw/*1722.36*/(""", """),_display_(/*1722.39*/{fName}),format.raw/*1722.46*/("""_output, """),_display_(/*1722.56*/{fName}),format.raw/*1722.63*/("""_input, """),_display_(/*1722.72*/{fName}),format.raw/*1722.79*/("""_inputTypes) ;
        """)))}),format.raw/*1723.10*/("""

        """),format.raw/*1725.9*/("""// List one or more processes to be composed into a system.
        system MasterA,
        """),_display_(/*1727.10*/{m.fmuNames.map(fName => s"${fName}_fmu").reduce[String]((a, b) => a + "," + b)}),format.raw/*1727.90*/(""",
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
                  SOURCE: src/main/twirl/CosimUppaalTemplate.scala.xml
                  HASH: 0d346a5ad97bacebbb5565d0547eca039322c11e
                  MATRIX: 262->2|640->67|751->86|785->94|2172->1453|2201->1454|2242->1467|2386->1583|2415->1584|2460->1601|2514->1627|2543->1628|2585->1642|2693->1722|2722->1723|2767->1740|2932->1877|2961->1878|3003->1892|3192->2053|3221->2054|3266->2071|3319->2096|3348->2097|3390->2111|3439->2133|3468->2134|3505->2144|3707->2318|3736->2319|3777->2332|3852->2380|3881->2381|3910->2382|4346->2791|4356->2792|4388->2803|4450->2838|4460->2839|4493->2851|4607->2938|4617->2939|4644->2945|4786->3060|4796->3061|4839->3083|4912->3129|4922->3130|4965->3152|5096->3256|5106->3257|5136->3266|5564->3667|5574->3668|5600->3673|5781->3825|5811->3826|5853->3839|6139->4097|6169->4098|6199->4099|6263->4134|6293->4135|6335->4148|6469->4254|6499->4255|6529->4256|6731->4429|6761->4430|6803->4443|6968->4580|6998->4581|7028->4582|7101->4626|7131->4627|7173->4640|7266->4705|7296->4706|7326->4707|9509->6862|9520->6863|9552->6873|9618->6911|9629->6912|9665->6926|9781->7014|9792->7015|9824->7025|10070->7242|10100->7244|10111->7245|10147->7259|10177->7260|10309->7363|10339->7364|10381->7377|10907->7874|10937->7875|10983->7892|11054->7934|11084->7935|11126->7948|11173->7966|11203->7967|11249->7984|11302->8008|11332->8009|11382->8030|11559->8178|11589->8179|11619->8180|11672->8204|11702->8205|11752->8226|11848->8293|11878->8294|11908->8295|11942->8300|11972->8301|12022->8322|12127->8398|12157->8399|12199->8412|12229->8413|12271->8426|12383->8510|12413->8511|12452->8522|12578->8620|12618->8643|12659->8645|12696->8654|12735->8665|12762->8670|12792->8671|12824->8675|12835->8676|12870->8689|12913->8700|12951->8710|13031->8762|13071->8785|13112->8787|13149->8796|13188->8807|13217->8814|13256->8825|13267->8826|13304->8841|13353->8862|13382->8869|13422->8881|13433->8882|13471->8898|13514->8909|13552->8919|13636->8975|13676->8998|13717->9000|13754->9009|13786->9013|13813->9018|13843->9019|13881->9029|13892->9030|13929->9045|13967->9055|14017->9088|14058->9090|14095->9099|14134->9110|14186->9140|14216->9141|14248->9145|14259->9146|14293->9170|14323->9178|14366->9189|14403->9198|14435->9202|14462->9207|14492->9208|14531->9219|14542->9220|14580->9236|14618->9246|14670->9281|14711->9283|14748->9292|14787->9303|14840->9334|14870->9335|14902->9339|14913->9340|14948->9365|14979->9374|15022->9385|15059->9394|15098->9405|15127->9412|15192->9448|15222->9449|15252->9451|15263->9452|15306->9473|15336->9474|15366->9475|15409->9486|15447->9496|15728->9748|15758->9749|15788->9751|15799->9752|15840->9771|15870->9772|15900->9773|16096->9940|16126->9941|16156->9943|16167->9944|16198->9953|16228->9954|16258->9955|16363->10031|16393->10032|16423->10034|16434->10035|16474->10053|16504->10054|16534->10055|16771->10263|16801->10264|16831->10266|16842->10267|16882->10285|16912->10286|16942->10287|17154->10470|17184->10471|17214->10473|17225->10474|17261->10488|17291->10489|17321->10490|17393->10534|17404->10535|17444->10553|17567->10648|17578->10649|17625->10674|17703->10724|17714->10725|17762->10751|17860->10820|17890->10822|17901->10823|17939->10839|17969->10840|18044->10887|18055->10888|18100->10911|18244->11027|18255->11028|18311->11062|18409->11132|18420->11133|18489->11179|18729->11389|18760->11391|18772->11392|18835->11432|18866->11433|19012->11549|19043->11551|19055->11552|19112->11586|19143->11587|19277->11693|19288->11694|19332->11716|19413->11769|19424->11770|19475->11799|19582->11877|19612->11879|19623->11880|19665->11900|19696->11901|19798->11974|19828->11976|19839->11977|19870->11986|19900->11987|20044->12103|20055->12104|20111->12138|20206->12205|20217->12206|20283->12249|20381->12319|20392->12320|20461->12366|20710->12585|20741->12587|20753->12588|20816->12628|20847->12629|21002->12754|21033->12756|21045->12757|21102->12791|21133->12792|21302->12931|21333->12933|21345->12934|21407->12973|21438->12974|21679->13186|21709->13187|21739->13189|21750->13190|21797->13214|21828->13215|21859->13216|21969->13297|21999->13298|22029->13300|22040->13301|22088->13326|22119->13327|22150->13328|22251->13400|22281->13401|22311->13403|22322->13404|22359->13419|22389->13420|22419->13421|23001->13974|23031->13975|23061->13977|23072->13978|23118->14001|23149->14002|23180->14003|23284->14078|23314->14079|23344->14081|23355->14082|23408->14112|23439->14113|23470->14114|23748->14362|23779->14364|23791->14365|23847->14398|23878->14399|23909->14400|24058->14519|24089->14521|24101->14522|24157->14555|24188->14556|24219->14557|24622->14930|24653->14932|24665->14933|24727->14972|24758->14973|24789->14974|24960->15115|24991->15117|25003->15118|25065->15157|25096->15158|25127->15159|25300->15302|25331->15304|25343->15305|25404->15343|25435->15344|25466->15345|25570->15420|25600->15421|25630->15423|25641->15424|25685->15446|25716->15447|25747->15448|25994->15666|26024->15667|26061->15676|26196->15783|26225->15784|26259->15790|26315->15817|26345->15818|26382->15827|26518->15935|26547->15936|26582->15943|26643->15975|26673->15976|26710->15985|26791->16037|26821->16038|26863->16051|26961->16121|26991->16122|27028->16131|27109->16184|27138->16185|27172->16191|27230->16220|27260->16221|27297->16230|27440->16345|27469->16346|27504->16353|27575->16395|27605->16396|27642->16405|27916->16651|27945->16652|27979->16658|28055->16705|28085->16706|28122->16715|28173->16737|28203->16738|28245->16751|28414->16892|28444->16893|28477->16897|28507->16898|28549->16911|28675->17009|28705->17010|28742->17019|28779->17028|28808->17029|28837->17030|37789->25953|37819->25954|37856->25963|38018->26097|38047->26098|38081->26104|38148->26142|38178->26143|38215->26152|38342->26250|38372->26251|38414->26264|38771->26593|38801->26594|38834->26599|38863->26600|38897->26606|38945->26625|38975->26626|39012->26635|39227->26822|39256->26823|39291->26830|39347->26857|39377->26858|39414->26867|39509->26934|39538->26935|39572->26941|39732->27072|39762->27073|39799->27082|40585->27840|40614->27841|40648->27847|40700->27870|40730->27871|40767->27880|40864->27949|40893->27950|40928->27957|40982->27982|41012->27983|41049->27992|41104->28018|41134->28019|41176->28032|41242->28070|41272->28071|41305->28076|41334->28077|41363->28078|45026->31712|45056->31713|45093->31722|45270->31871|45299->31872|45333->31878|45397->31913|45427->31914|45464->31923|45604->32035|45633->32036|45668->32043|45735->32081|45765->32082|45802->32091|45963->32223|45993->32224|46035->32237|46246->32419|46276->32420|46322->32437|46555->32641|46585->32642|46618->32646|46648->32647|46694->32664|46927->32868|46957->32869|46994->32878|47024->32879|47057->32884|47086->32885|47120->32891|47168->32910|47198->32911|47235->32920|47532->33189|47561->33190|47596->33197|47652->33224|47682->33225|47719->33234|47839->33326|47868->33327|47902->33333|48062->33464|48092->33465|48129->33474|49086->34403|49115->34404|49149->34410|49201->34433|49231->34434|49268->34443|49365->34512|49394->34513|49429->34520|49483->34545|49513->34546|49550->34555|49605->34581|49635->34582|49677->34595|49743->34633|49773->34634|49806->34639|49835->34640|49864->34641|55464->40211|55495->40212|55533->40221|55645->40304|55675->40305|55710->40311|55774->40345|55805->40346|55843->40355|55951->40434|55981->40435|56016->40441|56064->40459|56095->40460|56133->40469|56271->40577|56302->40578|56345->40591|56404->40620|56435->40621|56482->40638|56549->40675|56580->40676|56618->40685|56649->40686|56687->40695|56730->40709|56760->40710|56796->40717|56842->40733|56873->40734|56911->40743|57104->40907|57134->40908|57169->40914|57219->40934|57250->40935|57288->40944|57375->41002|57405->41003|57441->41010|57491->41030|57522->41031|57560->41040|57605->41055|57636->41056|57679->41069|57918->41279|57949->41280|57983->41285|58013->41286|58043->41287|62796->46010|62827->46011|62858->46013|62912->46044|62943->46045|62974->46046|63050->46092|63081->46093|63112->46095|63167->46127|63198->46128|63229->46129|63596->46466|63627->46467|63658->46469|63670->46470|63704->46481|63735->46482|63766->46483|63833->46520|63864->46521|63895->46523|63907->46524|63941->46535|63972->46536|64003->46537|64057->46561|64088->46562|64126->46571|64258->46673|64289->46674|64332->46687|64449->46775|64480->46776|64518->46785|64580->46817|64611->46818|64654->46831|64773->46921|64804->46922|64838->46927|64868->46928|64903->46934|64960->46961|64991->46962|65029->46971|65251->47164|65281->47165|65316->47171|65373->47198|65404->47199|65442->47208|65550->47286|65581->47287|65624->47300|65747->47392|65779->47393|65826->47410|65992->47546|66023->47547|66061->47556|66092->47557|66126->47562|66156->47563|66191->47569|66320->47668|66351->47669|66389->47678|66483->47742|66514->47743|66552->47752|66709->47880|66740->47881|66779->47891|66919->48001|66950->48002|66993->48015|67058->48050|67089->48051|67136->48068|67201->48103|67232->48104|67270->48113|67301->48114|67340->48124|67463->48217|67494->48218|67537->48231|67713->48377|67744->48378|67791->48395|67923->48497|67954->48498|67988->48502|68019->48503|68066->48520|68198->48622|68229->48623|68267->48632|68298->48633|68337->48643|68472->48749|68502->48750|68537->48756|68584->48773|68615->48774|68653->48783|68802->48903|68832->48904|68867->48910|68911->48924|68942->48925|68980->48934|69154->49079|69184->49080|69219->49086|69278->49115|69309->49116|69347->49125|69395->49143|69426->49144|69469->49157|69519->49178|69550->49179|69588->49188|70063->49634|70093->49635|70128->49641|70180->49663|70211->49664|70249->49673|70297->49691|70328->49692|70371->49705|70421->49726|70452->49727|70490->49736|70830->50047|70860->50048|70896->50055|70951->50080|70982->50081|71016->50086|71064->50104|71095->50105|71129->50110|71175->50127|71205->50128|71240->50134|72246->51111|72276->51112|72312->51119|72360->51137|72391->51138|72429->51147|72477->51165|72508->51166|72551->51179|72601->51200|72632->51201|72671->51211|73091->51602|73121->51603|73156->51609|73207->51630|73238->51631|73276->51640|73324->51658|73355->51659|73398->51672|73448->51693|73479->51694|73518->51704|74053->52210|74083->52211|74122->52221|74275->52344|74306->52345|74344->52354|74633->52614|74663->52615|74698->52621|74745->52638|74776->52639|74814->52648|74886->52691|74916->52692|74951->52698|75001->52718|75032->52719|75070->52728|75175->52804|75205->52805|75240->52811|75296->52837|75327->52838|75365->52847|75413->52865|75444->52866|75487->52879|75547->52909|75578->52910|75625->52927|75771->53043|75802->53044|75845->53057|75907->53089|75938->53090|75985->53107|76125->53217|76156->53218|76199->53231|76258->53261|76289->53262|76323->53266|76354->53267|76397->53280|76457->53310|76488->53311|76535->53328|76612->53375|76643->53376|76686->53389|76748->53421|76779->53422|76826->53439|76896->53479|76927->53480|76970->53493|77036->53530|77067->53531|77101->53536|77131->53537|77162->53539|87544->63892|87585->63915|87627->63917|87666->63927|87696->63934|87737->63946|87767->63953|87799->63956|87829->63963|87868->63973|87898->63980|87936->63989|87966->63996|88023->64020|88062->64030|88184->64123|88287->64203
                  LINES: 10->2|15->3|20->4|20->4|54->38|54->38|55->39|56->40|56->40|57->41|58->42|58->42|60->44|61->45|61->45|62->46|64->48|64->48|66->50|67->51|67->51|68->52|69->53|69->53|71->55|72->56|72->56|74->58|80->64|80->64|81->65|83->67|83->67|83->67|94->78|94->78|94->78|95->79|95->79|95->79|98->82|98->82|98->82|101->85|101->85|101->85|102->86|102->86|102->86|105->89|105->89|105->89|115->99|115->99|115->99|122->106|122->106|123->107|130->114|130->114|130->114|132->116|132->116|133->117|136->120|136->120|136->120|143->127|143->127|144->128|148->132|148->132|148->132|150->134|150->134|151->135|153->137|153->137|153->137|216->200|216->200|216->200|217->201|217->201|217->201|220->204|220->204|220->204|226->210|226->210|226->210|226->210|226->210|229->213|229->213|230->214|242->226|242->226|243->227|244->228|244->228|245->229|245->229|245->229|246->230|246->230|246->230|247->231|249->233|249->233|249->233|249->233|249->233|250->234|252->236|252->236|252->236|252->236|252->236|253->237|255->239|255->239|256->240|256->240|257->241|259->243|259->243|262->246|264->248|264->248|264->248|265->249|265->249|265->249|265->249|265->249|265->249|265->249|266->250|268->252|269->253|269->253|269->253|270->254|270->254|270->254|270->254|270->254|270->254|271->255|271->255|271->255|271->255|271->255|272->256|274->258|275->259|275->259|275->259|276->260|276->260|276->260|276->260|276->260|276->260|276->260|277->261|277->261|277->261|278->262|278->262|278->262|278->262|278->262|278->262|278->262|278->262|279->263|280->264|280->264|280->264|280->264|280->264|280->264|280->264|281->265|281->265|281->265|282->266|282->266|282->266|282->266|282->266|282->266|282->266|282->266|283->267|284->268|284->268|284->268|284->268|284->268|284->268|284->268|284->268|284->268|284->268|285->269|287->271|289->273|289->273|289->273|289->273|289->273|289->273|289->273|292->276|292->276|292->276|292->276|292->276|292->276|292->276|294->278|294->278|294->278|294->278|294->278|294->278|294->278|297->281|297->281|297->281|297->281|297->281|297->281|297->281|300->284|300->284|300->284|300->284|300->284|300->284|300->284|303->287|303->287|303->287|306->290|306->290|306->290|307->291|307->291|307->291|308->292|308->292|308->292|308->292|308->292|309->293|309->293|309->293|312->296|312->296|312->296|313->297|313->297|313->297|316->300|316->300|316->300|316->300|316->300|317->301|317->301|317->301|317->301|317->301|322->306|322->306|322->306|323->307|323->307|323->307|325->309|325->309|325->309|325->309|325->309|326->310|326->310|326->310|326->310|326->310|329->313|329->313|329->313|330->314|330->314|330->314|331->315|331->315|331->315|334->318|334->318|334->318|334->318|334->318|335->319|335->319|335->319|335->319|335->319|336->320|336->320|336->320|336->320|336->320|339->323|339->323|339->323|339->323|339->323|339->323|339->323|341->325|341->325|341->325|341->325|341->325|341->325|341->325|343->327|343->327|343->327|343->327|343->327|343->327|343->327|351->335|351->335|351->335|351->335|351->335|351->335|351->335|352->336|352->336|352->336|352->336|352->336|352->336|352->336|356->340|356->340|356->340|356->340|356->340|356->340|357->341|357->341|357->341|357->341|357->341|357->341|361->345|361->345|361->345|361->345|361->345|361->345|362->346|362->346|362->346|362->346|362->346|362->346|364->348|364->348|364->348|364->348|364->348|364->348|366->350|366->350|366->350|366->350|366->350|366->350|366->350|378->362|378->362|379->363|382->366|382->366|384->368|384->368|384->368|385->369|388->372|388->372|391->375|391->375|391->375|392->376|392->376|392->376|393->377|394->378|394->378|395->379|397->381|397->381|399->383|399->383|399->383|400->384|403->387|403->387|406->390|406->390|406->390|407->391|416->400|416->400|418->402|418->402|418->402|419->403|419->403|419->403|420->404|422->406|422->406|422->406|422->406|423->407|425->409|425->409|426->410|427->411|427->411|428->412|691->675|691->675|692->676|695->679|695->679|697->681|697->681|697->681|698->682|700->684|700->684|701->685|705->689|705->689|706->690|706->690|708->692|708->692|708->692|709->693|715->699|715->699|718->702|718->702|718->702|719->703|721->705|721->705|723->707|724->708|724->708|725->709|731->715|731->715|733->717|733->717|733->717|734->718|735->719|735->719|738->722|738->722|738->722|739->723|739->723|739->723|740->724|741->725|741->725|742->726|742->726|743->727|850->834|850->834|851->835|854->838|854->838|856->840|856->840|856->840|857->841|859->843|859->843|862->846|862->846|862->846|863->847|866->850|866->850|867->851|869->853|869->853|870->854|872->856|872->856|872->856|872->856|873->857|875->859|875->859|876->860|876->860|877->861|877->861|879->863|879->863|879->863|880->864|888->872|888->872|891->875|891->875|891->875|892->876|895->879|895->879|897->881|898->882|898->882|899->883|908->892|908->892|910->894|910->894|910->894|911->895|912->896|912->896|915->899|915->899|915->899|916->900|916->900|916->900|917->901|918->902|918->902|919->903|919->903|920->904|1072->1056|1072->1056|1073->1057|1075->1059|1075->1059|1077->1061|1077->1061|1077->1061|1078->1062|1080->1064|1080->1064|1082->1066|1082->1066|1082->1066|1083->1067|1086->1070|1086->1070|1087->1071|1087->1071|1087->1071|1088->1072|1089->1073|1089->1073|1090->1074|1090->1074|1091->1075|1092->1076|1092->1076|1095->1079|1095->1079|1095->1079|1096->1080|1098->1082|1098->1082|1100->1084|1100->1084|1100->1084|1101->1085|1102->1086|1102->1086|1105->1089|1105->1089|1105->1089|1106->1090|1106->1090|1106->1090|1107->1091|1110->1094|1110->1094|1111->1095|1111->1095|1112->1096|1250->1234|1250->1234|1250->1234|1250->1234|1250->1234|1250->1234|1251->1235|1251->1235|1251->1235|1251->1235|1251->1235|1251->1235|1265->1249|1265->1249|1265->1249|1265->1249|1265->1249|1265->1249|1265->1249|1266->1250|1266->1250|1266->1250|1266->1250|1266->1250|1266->1250|1266->1250|1268->1252|1268->1252|1269->1253|1271->1255|1271->1255|1272->1256|1274->1258|1274->1258|1275->1259|1275->1259|1275->1259|1276->1260|1278->1262|1278->1262|1279->1263|1279->1263|1281->1265|1281->1265|1281->1265|1282->1266|1287->1271|1287->1271|1289->1273|1289->1273|1289->1273|1290->1274|1291->1275|1291->1275|1292->1276|1292->1276|1292->1276|1293->1277|1294->1278|1294->1278|1295->1279|1295->1279|1296->1280|1296->1280|1298->1282|1299->1283|1299->1283|1300->1284|1301->1285|1301->1285|1302->1286|1304->1288|1304->1288|1306->1290|1311->1295|1311->1295|1312->1296|1312->1296|1312->1296|1313->1297|1314->1298|1314->1298|1315->1299|1315->1299|1317->1301|1318->1302|1318->1302|1319->1303|1320->1304|1320->1304|1321->1305|1323->1307|1323->1307|1323->1307|1323->1307|1324->1308|1326->1310|1326->1310|1327->1311|1327->1311|1329->1313|1333->1317|1333->1317|1335->1319|1335->1319|1335->1319|1336->1320|1339->1323|1339->1323|1341->1325|1341->1325|1341->1325|1342->1326|1346->1330|1346->1330|1348->1332|1348->1332|1348->1332|1349->1333|1349->1333|1349->1333|1350->1334|1351->1335|1351->1335|1352->1336|1356->1340|1356->1340|1358->1342|1358->1342|1358->1342|1359->1343|1359->1343|1359->1343|1360->1344|1361->1345|1361->1345|1362->1346|1365->1349|1365->1349|1368->1352|1368->1352|1368->1352|1369->1353|1369->1353|1369->1353|1370->1354|1371->1355|1371->1355|1373->1357|1380->1364|1380->1364|1383->1367|1383->1367|1383->1367|1384->1368|1384->1368|1384->1368|1385->1369|1386->1370|1386->1370|1388->1372|1391->1375|1391->1375|1393->1377|1393->1377|1393->1377|1394->1378|1394->1378|1394->1378|1395->1379|1396->1380|1396->1380|1398->1382|1402->1386|1402->1386|1404->1388|1405->1389|1405->1389|1406->1390|1409->1393|1409->1393|1411->1395|1411->1395|1411->1395|1412->1396|1414->1398|1414->1398|1416->1400|1416->1400|1416->1400|1417->1401|1419->1403|1419->1403|1421->1405|1421->1405|1421->1405|1422->1406|1422->1406|1422->1406|1423->1407|1423->1407|1423->1407|1424->1408|1426->1410|1426->1410|1427->1411|1427->1411|1427->1411|1428->1412|1430->1414|1430->1414|1431->1415|1432->1416|1432->1416|1432->1416|1432->1416|1433->1417|1433->1417|1433->1417|1434->1418|1435->1419|1435->1419|1436->1420|1436->1420|1436->1420|1437->1421|1438->1422|1438->1422|1439->1423|1440->1424|1440->1424|1441->1425|1441->1425|1443->1427|1737->1721|1737->1721|1737->1721|1738->1722|1738->1722|1738->1722|1738->1722|1738->1722|1738->1722|1738->1722|1738->1722|1738->1722|1738->1722|1739->1723|1741->1725|1743->1727|1743->1727
                  -- GENERATED --
              */
          

package xml

import _root_.play.twirl.api.JavaScript
import _root_.play.twirl.api.Xml
import _root_.play.twirl.api.Html
import _root_.play.twirl.api.TwirlHelperImports._
import _root_.play.twirl.api.TwirlFeatureImports._
import _root_.play.twirl.api.Txt
/*1.2*/import org.intocps.verification.scenarioverifier.core._

object DynamicCosimUppaalTemplateNoEnabled extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.XmlFormat.Appendable,_root_.play.twirl.api.Format[play.twirl.api.XmlFormat.Appendable]](play.twirl.api.XmlFormat) with _root_.play.twirl.api.Template1[ModelEncoding,play.twirl.api.XmlFormat.Appendable] {

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
        bool shouldChecksBeDisabled()"""),format.raw/*35.38*/("""{"""),format.raw/*35.39*/("""
            """),format.raw/*36.13*/("""//In case a loop is not activated all checks should be
            if(loopActive == -1 &amp;&amp; !stepFinderActive)"""),format.raw/*37.62*/("""{"""),format.raw/*37.63*/("""
                """),format.raw/*38.17*/("""return false;
            """),format.raw/*39.13*/("""}"""),format.raw/*39.14*/("""

            """),format.raw/*41.13*/("""//We are inside a loop is it nested
            if(isLoopNested || isStepNested)"""),format.raw/*42.45*/("""{"""),format.raw/*42.46*/("""
                """),format.raw/*43.17*/("""//Both loops should be on the extraIteration
                return !(isStepExtraIteration &amp;&amp; isLoopExtraIteration);
            """),format.raw/*45.13*/("""}"""),format.raw/*45.14*/("""

            """),format.raw/*47.13*/("""//Not nested - if none of the loops is in the extra iteration we should disable the checks
            if(!isLoopExtraIteration &amp;&amp; !isStepExtraIteration)"""),format.raw/*48.71*/("""{"""),format.raw/*48.72*/("""
                """),format.raw/*49.17*/("""return true;
            """),format.raw/*50.13*/("""}"""),format.raw/*50.14*/("""

            """),format.raw/*52.13*/("""return false;
        """),format.raw/*53.9*/("""}"""),format.raw/*53.10*/("""

        """),format.raw/*55.9*/("""//FMU of a variable
        const int undefined := 0;
        const int defined := 1;
        const int notStable :=-1;

        //FMU of the variable
        typedef struct """),format.raw/*61.24*/("""{"""),format.raw/*61.25*/("""
            """),format.raw/*62.13*/("""int[-1,1] status;
            int time;
        """),format.raw/*64.9*/("""}"""),format.raw/*64.10*/(""" """),format.raw/*64.11*/("""variable;


        //Const assignment types - to future variables or current:
        const int final := 0;
        const int tentative := 1;
        const int noCommitment := -1;

        //***********************************************************************************************************

        //Max number of inputs/outputs any FMU can have - Should be changed
        const int MaxNInputs = """),_display_(/*75.33*/m/*75.34*/.maxNInputs),format.raw/*75.45*/(""";
        const int MaxNOutputs = """),_display_(/*76.34*/m/*76.35*/.maxNOutputs),format.raw/*76.47*/(""";

        //Numbers of FMUs in scenario - Should be changed
        const int nFMU = """),_display_(/*79.27*/m/*79.28*/.nFMUs),format.raw/*79.34*/(""";

        //number of algebraic loops in scenario - Should be changed
        const int nAlgebraicLoopsInStep := """),_display_(/*82.45*/m/*82.46*/.nAlgebraicLoopsInStep),format.raw/*82.68*/(""";

        //Adaptive co-simulation - numbers of different configurations
        const int nConfig := """),_display_(/*85.31*/m/*85.32*/.nConfigs),format.raw/*85.41*/(""";
        //***********************************************************************************************************
        //Do not change

        const int NActions := 14;

        //The number of actions in our system
        const int N := MaxNInputs &gt; MaxNOutputs? MaxNInputs : MaxNOutputs;

        //The maximum step allowed in system - shouldn't be changed
        const int H_max := """),_display_(/*95.29*/m/*95.30*/.Hmax),format.raw/*95.35*/(""";
        const int H := H_max;

        const int noStep := -1;
        const int noFMU := -1;
        const int noLoop := -1;

        typedef struct """),format.raw/*102.24*/("""{"""),format.raw/*102.25*/("""
            """),format.raw/*103.13*/("""int[-1, nFMU] FMU;
            int[-1,NActions] act;
            int[-1,N] portVariable;
            int[-1,H] step_size;
            int[-1,nFMU] relative_step_size;
            int[-1,1] commitment;
            int[-1, nAlgebraicLoopsInStep] loop;
        """),format.raw/*110.9*/("""}"""),format.raw/*110.10*/(""" """),format.raw/*110.11*/("""Operation;

        typedef struct """),format.raw/*112.24*/("""{"""),format.raw/*112.25*/("""
            """),format.raw/*113.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNInputs] input;
            int[-1, MaxNOutputs] output;
        """),format.raw/*116.9*/("""}"""),format.raw/*116.10*/(""" """),format.raw/*116.11*/("""InternalConnection;

        //Types of input ports
        const int delayed := 0;
        const int reactive := 1;
        const int noPort := -1;

        typedef struct """),format.raw/*123.24*/("""{"""),format.raw/*123.25*/("""
            """),format.raw/*124.13*/("""int[0, nFMU] SrcFMU;
            int[0,MaxNOutputs] output;
            int[0,nFMU] TrgFMU;
            int[0,MaxNInputs] input;
        """),format.raw/*128.9*/("""}"""),format.raw/*128.10*/(""" """),format.raw/*128.11*/("""ExternalConnection;

        typedef struct """),format.raw/*130.24*/("""{"""),format.raw/*130.25*/("""
            """),format.raw/*131.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNOutputs] port;
        """),format.raw/*133.9*/("""}"""),format.raw/*133.10*/(""" """),format.raw/*133.11*/("""FmuOutputPort;


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

        //***********************************************************************************************************
        //Scenario Dependent - Should be changed!

        //Number of internal connections - both init and normal
        const int nInternal := """),_display_(/*190.33*/m/*190.34*/.nInternal),format.raw/*190.44*/(""";

        //Number of external connections in scenario
        const int nExternal := """),_display_(/*193.33*/m/*193.34*/.nExternal),format.raw/*193.44*/(""";

        //The initial of value of h
        int h := H_max;

        //This array is representing the variables of the stepSize that each FMU can take - H_max is the default value
        int stepVariables[nFMU] = """),format.raw/*199.35*/("""{"""),_display_(/*199.37*/m/*199.38*/.stepVariables),format.raw/*199.52*/("""}"""),format.raw/*199.53*/(""";

        //A generic action to pick the next action
        void unpackOperation(Operation operation)"""),format.raw/*202.50*/("""{"""),format.raw/*202.51*/("""
            """),format.raw/*203.13*/("""//action to be performed
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
            if(loopActive == noLoop)"""),format.raw/*215.37*/("""{"""),format.raw/*215.38*/("""
                """),format.raw/*216.17*/("""loopActive := operation.loop;
            """),format.raw/*217.13*/("""}"""),format.raw/*217.14*/("""
            """),format.raw/*218.13*/("""if(action == step)"""),format.raw/*218.31*/("""{"""),format.raw/*218.32*/("""
                """),format.raw/*219.17*/("""if (stepsize == noStep) """),format.raw/*219.41*/("""{"""),format.raw/*219.42*/("""
                    """),format.raw/*220.21*/("""// Step is relative to the fmu referred to by relative_step_size
                    stepsize := stepVariables[relative_step_size];
                """),format.raw/*222.17*/("""}"""),format.raw/*222.18*/(""" """),format.raw/*222.19*/("""else if (stepsize == H) """),format.raw/*222.43*/("""{"""),format.raw/*222.44*/("""
                    """),format.raw/*223.21*/("""// Default step
                    stepsize := h;
                """),format.raw/*225.17*/("""}"""),format.raw/*225.18*/(""" """),format.raw/*225.19*/("""else """),format.raw/*225.24*/("""{"""),format.raw/*225.25*/("""
                    """),format.raw/*226.21*/("""// Absolute step size
                    // Nothing to do.
                """),format.raw/*228.17*/("""}"""),format.raw/*228.18*/("""
            """),format.raw/*229.13*/("""}"""),format.raw/*229.14*/("""
            """),format.raw/*230.13*/("""//Update checkStatus
            checksDisabled = shouldChecksBeDisabled();
        """),format.raw/*232.9*/("""}"""),format.raw/*232.10*/("""


        """),format.raw/*235.9*/("""//Encoding of the scenario
        //Each FMU should have a different ID \in [0, nFMU-1]
        """),_display_(/*237.10*/for(fName<- m.fmuNames) yield /*237.33*/ {_display_(Seq[Any](format.raw/*237.35*/("""
        """),format.raw/*238.9*/("""const int """),_display_(/*238.20*/fName),format.raw/*238.25*/(""" """),format.raw/*238.26*/(""":= """),_display_(/*238.30*/m/*238.31*/.fmuId(fName)),format.raw/*238.44*/(""";
        """)))}),format.raw/*239.10*/("""

        """),format.raw/*241.9*/("""//Number of inputs and outputs of each FMU
        """),_display_(/*242.10*/for(fName<- m.fmuNames) yield /*242.33*/ {_display_(Seq[Any](format.raw/*242.35*/("""
        """),format.raw/*243.9*/("""const int """),_display_(/*243.20*/{fName}),format.raw/*243.27*/("""_input := """),_display_(/*243.38*/m/*243.39*/.nInputs(fName)),format.raw/*243.54*/(""";
        const int """),_display_(/*244.20*/{fName}),format.raw/*244.27*/("""_output := """),_display_(/*244.39*/m/*244.40*/.nOutputs(fName)),format.raw/*244.56*/(""";
        """)))}),format.raw/*245.10*/("""

        """),format.raw/*247.9*/("""//Definition of inputs and outputs of each FMU
        """),_display_(/*248.10*/for(fName<- m.fmuNames) yield /*248.33*/ {_display_(Seq[Any](format.raw/*248.35*/("""
        """),format.raw/*249.9*/("""// """),_display_(/*249.13*/fName),format.raw/*249.18*/(""" """),format.raw/*249.19*/("""inputs - """),_display_(/*249.29*/m/*249.30*/.nInputs(fName)),format.raw/*249.45*/("""
        """),_display_(/*250.10*/for(inName<- m.fmuInNames(fName)) yield /*250.43*/ {_display_(Seq[Any](format.raw/*250.45*/("""
        """),format.raw/*251.9*/("""const int """),_display_(/*251.20*/{m.fmuPortName(fName, inName)}),format.raw/*251.50*/(""" """),format.raw/*251.51*/(""":= """),_display_(/*251.55*/m/*251.56*/.fmuInputEncoding(fName)/*251.80*/(inName)),format.raw/*251.88*/(""";
        """)))}),format.raw/*252.10*/("""
        """),format.raw/*253.9*/("""// """),_display_(/*253.13*/fName),format.raw/*253.18*/(""" """),format.raw/*253.19*/("""outputs - """),_display_(/*253.30*/m/*253.31*/.nOutputs(fName)),format.raw/*253.47*/("""
        """),_display_(/*254.10*/for(outName<- m.fmuOutNames(fName)) yield /*254.45*/ {_display_(Seq[Any](format.raw/*254.47*/("""
        """),format.raw/*255.9*/("""const int """),_display_(/*255.20*/{m.fmuPortName(fName, outName)}),format.raw/*255.51*/(""" """),format.raw/*255.52*/(""":= """),_display_(/*255.56*/m/*255.57*/.fmuOutputEncoding(fName)/*255.82*/(outName)),format.raw/*255.91*/(""";
        """)))}),format.raw/*256.10*/("""
        """),format.raw/*257.9*/("""const int """),_display_(/*257.20*/{fName}),format.raw/*257.27*/("""_inputTypes[nConfig][MaxNInputs] := """),format.raw/*257.63*/("""{"""),format.raw/*257.64*/(""" """),_display_(/*257.66*/m/*257.67*/.fmuInputTypes(fName)),format.raw/*257.88*/(""" """),format.raw/*257.89*/("""}"""),format.raw/*257.90*/(""";
        """)))}),format.raw/*258.10*/("""

        """),format.raw/*260.9*/("""//This array is to keep track of the value of each output port - each output port needs two variables (current and future)
        // and each variable is having two values (defined and time)
        variable connectionVariable[nFMU][MaxNOutputs][2] = """),format.raw/*262.61*/("""{"""),format.raw/*262.62*/(""" """),_display_(/*262.64*/m/*262.65*/.connectionVariable),format.raw/*262.84*/(""" """),format.raw/*262.85*/("""}"""),format.raw/*262.86*/(""";

        //Connections - do not longer contain the type of the input - but it is still a 1:1 mapping
        const ExternalConnection external[nConfig][nExternal] = """),format.raw/*265.65*/("""{"""),format.raw/*265.66*/(""" """),_display_(/*265.68*/m/*265.69*/.external),format.raw/*265.78*/(""" """),format.raw/*265.79*/("""}"""),format.raw/*265.80*/(""";

        const InternalConnection feedthroughInStep[nConfig][nInternal] = """),format.raw/*267.74*/("""{"""),format.raw/*267.75*/(""" """),_display_(/*267.77*/m/*267.78*/.feedthroughInStep),format.raw/*267.96*/(""" """),format.raw/*267.97*/("""}"""),format.raw/*267.98*/(""";

        //The array show if an FMU can reject a step or not - if the FMU can reject a step the value is 1 on the index defined by the fmus
        const bool mayRejectStep[nFMU] = """),format.raw/*270.42*/("""{"""),format.raw/*270.43*/(""" """),_display_(/*270.45*/m/*270.46*/.mayRejectStep),format.raw/*270.60*/(""" """),format.raw/*270.61*/("""}"""),format.raw/*270.62*/(""";


        const int maxStepOperations := """),_display_(/*273.41*/m/*273.42*/.maxStepOperations),format.raw/*273.60*/(""";

        //Numbers of operations in each step
        const int[0,maxStepOperations] nStepOperations[nConfig] := """),format.raw/*276.68*/("""{"""),_display_(/*276.70*/m/*276.71*/.nStepOperations),format.raw/*276.87*/("""}"""),format.raw/*276.88*/(""";

        // Number of operations in the step finding loop
        const int maxFindStepOperations := """),_display_(/*279.45*/m/*279.46*/.maxFindStepOperations),format.raw/*279.68*/(""";
        const int maxFindStepRestoreOperations := """),_display_(/*280.52*/m/*280.53*/.maxFindStepRestoreOperations),format.raw/*280.82*/(""";

        const int[0,maxFindStepOperations] nFindStepOperations[nConfig] := """),format.raw/*282.76*/("""{"""),_display_(/*282.78*/m/*282.79*/.nFindStepOperations),format.raw/*282.99*/("""}"""),format.raw/*282.100*/(""";
        const int[0,maxFindStepRestoreOperations] nRestore[nConfig] := """),format.raw/*283.72*/("""{"""),_display_(/*283.74*/m/*283.75*/.nRestore),format.raw/*283.84*/("""}"""),format.raw/*283.85*/(""";

        // Numbers for algebraic loop operations in step
        const int maxNAlgebraicLoopOperationsInStep := """),_display_(/*286.57*/m/*286.58*/.maxNAlgebraicLoopOperationsInStep),format.raw/*286.92*/(""";
        const int maxNRetryOperationsForAlgebraicLoopsInStep := """),_display_(/*287.66*/m/*287.67*/.maxNRetryOperationsForAlgebraicLoopsInStep),format.raw/*287.110*/(""";
        const int maxNConvergeOperationsForAlgebraicLoopsInStep := """),_display_(/*288.69*/m/*288.70*/.maxNConvergeOperationsForAlgebraicLoopsInStep),format.raw/*288.116*/(""";

        //Numbers of operations to be performed per algebraic loop in step
        const int[0,maxNConvergeOperationsForAlgebraicLoopsInStep] nConvergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*291.142*/("""{"""),_display_(/*291.144*/m/*291.145*/.nConvergencePortsPerAlgebraicLoopInStep),format.raw/*291.185*/("""}"""),format.raw/*291.186*/(""";
        const int[0,maxNAlgebraicLoopOperationsInStep] nOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*292.124*/("""{"""),_display_(/*292.126*/m/*292.127*/.nOperationsPerAlgebraicLoopInStep),format.raw/*292.161*/("""}"""),format.raw/*292.162*/(""";
        const int[0,maxNRetryOperationsForAlgebraicLoopsInStep] nRetryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*293.138*/("""{"""),_display_(/*293.140*/m/*293.141*/.nRetryOperationsPerAlgebraicLoopInStep),format.raw/*293.180*/("""}"""),format.raw/*293.181*/(""";

        //These operations define what should be performed in the simulation - it is assumed that the operation first loads the fmus
        const Operation stepOperations[nConfig][maxStepOperations] = """),format.raw/*296.70*/("""{"""),format.raw/*296.71*/(""" """),_display_(/*296.73*/m/*296.74*/.stepOperations),format.raw/*296.89*/(""" """),format.raw/*296.90*/("""}"""),format.raw/*296.91*/(""";

        //These are the operations to be performed in order to find the correct step
        //In these operation there is a difference on the third parameter to doStep:
        // H (A step-value greater than the allowed step (Greater than the number of FMUS)) means that we should look at the variable h
        // A stepSize (0:(nFMU-1)) means that the should look at that index in stepVariables use that as the step
        //This is being done inside - findStepAction

        const Operation findStepIteration[nConfig][maxFindStepOperations] = """),format.raw/*304.77*/("""{"""),format.raw/*304.78*/(""" """),_display_(/*304.80*/m/*304.81*/.findStepLoopOperations),format.raw/*304.104*/(""" """),format.raw/*304.105*/("""}"""),format.raw/*304.106*/(""";
        const Operation StepFix[nConfig][maxFindStepRestoreOperations] = """),format.raw/*305.74*/("""{"""),format.raw/*305.75*/(""" """),_display_(/*305.77*/m/*305.78*/.findStepLoopRestoreOperations),format.raw/*305.108*/(""" """),format.raw/*305.109*/("""}"""),format.raw/*305.110*/(""";

        //Possible multiple loops
        //Loop operations are to solve algebraic loops in the co-simulation scenario
        const Operation operationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNAlgebraicLoopOperationsInStep] = """),format.raw/*309.127*/("""{"""),_display_(/*309.129*/m/*309.130*/.operationsPerAlgebraicLoopInStep),format.raw/*309.163*/(""" """),format.raw/*309.164*/("""}"""),format.raw/*309.165*/(""";

        //The converge ports is to mark which variables that needs to be checked in the convergence loop
        //The convention is now to specify the FMU first and the port to denote the variables that should be checked
        const FmuOutputPort convergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNConvergeOperationsForAlgebraicLoopsInStep] = """),format.raw/*313.149*/("""{"""),_display_(/*313.151*/m/*313.152*/.convergencePortsPerAlgebraicLoopInStep),format.raw/*313.191*/(""" """),format.raw/*313.192*/("""}"""),format.raw/*313.193*/(""";

        const Operation retryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNRetryOperationsForAlgebraicLoopsInStep] = """),format.raw/*315.141*/("""{"""),_display_(/*315.143*/m/*315.144*/.retryOperationsPerAlgebraicLoopInStep),format.raw/*315.182*/(""" """),format.raw/*315.183*/("""}"""),format.raw/*315.184*/(""";

    </declaration>
<template>
<name>Interpreter</name>
<declaration>
    int inst_pc := 0;
    int cosimstep_pc := 0;
    int n := 0;

    void selectNextCosimStepAction()"""),format.raw/*325.37*/("""{"""),format.raw/*325.38*/("""
        """),format.raw/*326.9*/("""if(cosimstep_pc &lt; nStepOperations[currentConfig])"""),format.raw/*326.61*/("""{"""),format.raw/*326.62*/("""
            """),format.raw/*327.13*/("""unpackOperation(stepOperations[currentConfig][cosimstep_pc]);
        """),format.raw/*328.9*/("""}"""),format.raw/*328.10*/("""
        """),format.raw/*329.9*/("""//Proceed to next action
        cosimstep_pc++;
    """),format.raw/*331.5*/("""}"""),format.raw/*331.6*/("""
"""),format.raw/*332.1*/("""</declaration>
    <location id="id0" x="1674" y="187">
        <name x="1700" y="178">Terminated</name>
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
    <location id="id8" x="1674" y="-102">
        <name x="1691" y="-68">Simulate</name>
        <committed/>
    </location>
    <init ref="id6"/>
    <transition>
        <source ref="id8"/>
        <target ref="id0"/>
        <label kind="guard" x="1691" y="102">cosimstep_pc == nStepOperations[currentConfig] + 1 </label>
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
        <target ref="id8"/>
        <label kind="synchronisation" x="1784" y="-26">solveLoop?</label>
        <label kind="assignment" x="1801" y="8">selectNextCosimStepAction()</label>
    </transition>
    <transition>
        <source ref="id8"/>
        <target ref="id4"/>
        <label kind="guard" x="1903" y="-93">loopActive != -1
            &amp;&amp; action == loop</label>
        <label kind="synchronisation" x="1818" y="-127">solveLoop!</label>
        <nail x="2065" y="-110"/>
    </transition>
    <transition>
        <source ref="id5"/>
        <target ref="id8"/>
        <label kind="synchronisation" x="1861" y="-416">findStepChan?</label>
        <label kind="assignment" x="1861" y="-391">selectNextCosimStepAction(),
            stepFinderActive := false</label>
        <nail x="1946" y="-365"/>
    </transition>
    <transition>
        <source ref="id8"/>
        <target ref="id5"/>
        <label kind="guard" x="1844" y="-221">action == findStep</label>
        <label kind="synchronisation" x="2005" y="-170">findStepChan!</label>
        <label kind="assignment" x="1997" y="-195">stepFinderActive := true</label>
    </transition>
    <transition>
        <source ref="id6"/>
        <target ref="id8"/>
        <label kind="assignment" x="1326" y="-161">selectNextCosimStepAction(), isSimulation = 1</label>
        <nail x="1521" y="-102"/>
    </transition>
    <transition>
        <source ref="id7"/>
        <target ref="id8"/>
        <label kind="synchronisation" x="1631" y="-493">actionPerformed?</label>
        <label kind="assignment" x="1631" y="-467">selectNextCosimStepAction()</label>
        <nail x="1716" y="-442"/>
    </transition>
    <transition>
        <source ref="id8"/>
        <target ref="id7"/>
        <label kind="guard" x="1334" y="-382">(action == get ||
            action == set ||
            action == step ||
            action == save ||
            action == restore)
            &amp;&amp; cosimstep_pc &lt; (nStepOperations[currentConfig] +1)</label>
        <label kind="synchronisation" x="1470" y="-255">fmu[activeFMU]!</label>
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

    void selectNextLoopAction(int l)"""),format.raw/*439.37*/("""{"""),format.raw/*439.38*/("""
        """),format.raw/*440.9*/("""unpackOperation(operationsPerAlgebraicLoopInStep[currentConfig][l][convergence_pc]);
        //Proceed to next action
        convergence_pc ++;
    """),format.raw/*443.5*/("""}"""),format.raw/*443.6*/("""

    """),format.raw/*445.5*/("""void selectNextRestoreAction(int l)"""),format.raw/*445.40*/("""{"""),format.raw/*445.41*/("""
        """),format.raw/*446.9*/("""unpackOperation(retryOperationsPerAlgebraicLoopInStep[currentConfig][l][restore_pc]);
        restore_pc++;
    """),format.raw/*448.5*/("""}"""),format.raw/*448.6*/("""

    """),format.raw/*450.5*/("""void updateConvergenceVariables(int l)"""),format.raw/*450.43*/("""{"""),format.raw/*450.44*/("""
        """),format.raw/*451.9*/("""int fmu;
        int v;
        int i = 0;
        for(i = 0; i &lt; nConvergencePortsPerAlgebraicLoopInStep[currentConfig][l]; i++)"""),format.raw/*454.90*/("""{"""),format.raw/*454.91*/("""
            """),format.raw/*455.13*/("""fmu = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].FMU;
            v = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].port;
            if(isFeedthrough)"""),format.raw/*457.30*/("""{"""),format.raw/*457.31*/("""
                """),format.raw/*458.17*/("""connectionVariable[fmu][v][tentative].status := connectionVariable[fmu][v][final].status;
                connectionVariable[fmu][v][tentative].time := connectionVariable[fmu][v][final].time;
            """),format.raw/*460.13*/("""}"""),format.raw/*460.14*/("""else"""),format.raw/*460.18*/("""{"""),format.raw/*460.19*/("""
                """),format.raw/*461.17*/("""connectionVariable[fmu][v][final].status := connectionVariable[fmu][v][tentative].status;
                connectionVariable[fmu][v][final].time := connectionVariable[fmu][v][tentative].time;
            """),format.raw/*463.13*/("""}"""),format.raw/*463.14*/("""
        """),format.raw/*464.9*/("""}"""),format.raw/*464.10*/("""
    """),format.raw/*465.5*/("""}"""),format.raw/*465.6*/("""

    """),format.raw/*467.5*/("""void loopConverge()"""),format.raw/*467.24*/("""{"""),format.raw/*467.25*/("""
        """),format.raw/*468.9*/("""//Loop not longer active
        loopActive := -1;
        //Loop action counter reset
        convergence_pc := 0;
        //Reset convergence counter
        currentConvergeLoopIteration := 0;
        isLoopExtraIteration:= false;
        isFeedthrough := false;
    """),format.raw/*476.5*/("""}"""),format.raw/*476.6*/("""


    """),format.raw/*479.5*/("""void resetConvergenceloop()"""),format.raw/*479.32*/("""{"""),format.raw/*479.33*/("""
        """),format.raw/*480.9*/("""convergence_pc := 0;
        restore_pc := 0;
        selectNextLoopAction(loopActive);
    """),format.raw/*483.5*/("""}"""),format.raw/*483.6*/("""

    """),format.raw/*485.5*/("""//Convergence will happen when all convergenceVariables have a similar future and current value
    bool convergenceCriteria(int l)"""),format.raw/*486.36*/("""{"""),format.raw/*486.37*/("""
        """),format.raw/*487.9*/("""return forall(x:int[0,maxNConvergeOperationsForAlgebraicLoopsInStep-1])
            convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU != noFMU imply
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].status
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].status
            &amp;&amp;
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].time
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].time;
    """),format.raw/*496.5*/("""}"""),format.raw/*496.6*/("""

    """),format.raw/*498.5*/("""bool convergence(int l)"""),format.raw/*498.28*/("""{"""),format.raw/*498.29*/("""
        """),format.raw/*499.9*/("""return (convergenceCriteria(l) &amp;&amp; isLoopExtraIteration);
    """),format.raw/*500.5*/("""}"""),format.raw/*500.6*/("""


    """),format.raw/*503.5*/("""void updateIsExtra(int l)"""),format.raw/*503.30*/("""{"""),format.raw/*503.31*/("""
        """),format.raw/*504.9*/("""if(convergenceCriteria(l))"""),format.raw/*504.35*/("""{"""),format.raw/*504.36*/("""
            """),format.raw/*505.13*/("""isLoopExtraIteration := true;
        """),format.raw/*506.9*/("""}"""),format.raw/*506.10*/("""
    """),format.raw/*507.5*/("""}"""),format.raw/*507.6*/("""
"""),format.raw/*508.1*/("""</declaration>
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

    void selectNextStepFinderAction()"""),format.raw/*660.38*/("""{"""),format.raw/*660.39*/("""
        """),format.raw/*661.9*/("""unpackOperation(findStepIteration[currentConfig][step_pc]);
        step_pc++;
    """),format.raw/*663.5*/("""}"""),format.raw/*663.6*/("""

    """),format.raw/*665.5*/("""void selectNextStepRestoreAction()"""),format.raw/*665.39*/("""{"""),format.raw/*665.40*/("""
        """),format.raw/*666.9*/("""unpackOperation(StepFix[currentConfig][restore_pc]);
        restore_pc++;
    """),format.raw/*668.5*/("""}"""),format.raw/*668.6*/("""

    """),format.raw/*670.5*/("""void findMinStep()"""),format.raw/*670.23*/("""{"""),format.raw/*670.24*/("""
        """),format.raw/*671.9*/("""//Maximum step size allowed
        int min = nFMU;
        int j := 0;
        for(j = 0; j &lt; nFMU; j++)"""),format.raw/*674.37*/("""{"""),format.raw/*674.38*/("""
            """),format.raw/*675.13*/("""if(stepVariables[j] &lt; min)"""),format.raw/*675.42*/("""{"""),format.raw/*675.43*/("""
                """),format.raw/*676.17*/("""min := stepVariables[j];
            """),format.raw/*677.13*/("""}"""),format.raw/*677.14*/("""
        """),format.raw/*678.9*/("""}"""),format.raw/*678.10*/("""
        """),format.raw/*679.9*/("""h := min;
    """),format.raw/*680.5*/("""}"""),format.raw/*680.6*/("""


    """),format.raw/*683.5*/("""bool stepFound()"""),format.raw/*683.21*/("""{"""),format.raw/*683.22*/("""
        """),format.raw/*684.9*/("""//All FMU that may reject a step should be able to take the same step - h
        return forall(x:int[0, nFMU-1]) mayRejectStep[x] imply stepVariables[x] == h;
    """),format.raw/*686.5*/("""}"""),format.raw/*686.6*/("""

    """),format.raw/*688.5*/("""bool loopConverged()"""),format.raw/*688.25*/("""{"""),format.raw/*688.26*/("""
        """),format.raw/*689.9*/("""return (stepFound() &amp;&amp; isStepExtraIteration);
    """),format.raw/*690.5*/("""}"""),format.raw/*690.6*/("""


    """),format.raw/*693.5*/("""void updateIsExtra()"""),format.raw/*693.25*/("""{"""),format.raw/*693.26*/("""
        """),format.raw/*694.9*/("""if(stepFound())"""),format.raw/*694.24*/("""{"""),format.raw/*694.25*/("""
            """),format.raw/*695.13*/("""isStepExtraIteration := true;
            //Reset numbers of tries to 0 - This is to avoid problems with the maximum number of tries and not to active the nested checks
            numbersOfTries := 0;
        """),format.raw/*698.9*/("""}"""),format.raw/*698.10*/("""
    """),format.raw/*699.5*/("""}"""),format.raw/*699.6*/("""
"""),format.raw/*700.1*/("""</declaration>
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
    variable inputVariables[MaxNInputs] = """),format.raw/*838.43*/("""{"""),format.raw/*838.44*/(""" """),_display_(/*838.46*/{m.variableArray(m.maxNInputs)}),format.raw/*838.77*/(""" """),format.raw/*838.78*/("""}"""),format.raw/*838.79*/(""";
    variable outputVariables[MaxNOutputs] = """),format.raw/*839.45*/("""{"""),format.raw/*839.46*/(""" """),_display_(/*839.48*/{m.variableArray(m.maxNOutputs)}),format.raw/*839.80*/(""" """),format.raw/*839.81*/("""}"""),format.raw/*839.82*/(""";
    //Index for the for-loop
    int i := 0;

    //Backup FMU
    variable savedOutputVariables[MaxNOutputs];
    variable savedInputVariables[MaxNInputs];
    int savedTime;
    bool isSaved := false;
    bool isConsistent := true;

    void getValue(int v, int a)"""),format.raw/*850.32*/("""{"""),format.raw/*850.33*/("""
        """),format.raw/*851.9*/("""outputVariables[v].status := defined;
        outputVariables[v].time := cTime;

        connectionVariable[id][v][a].status := defined;
        connectionVariable[id][v][a].time := cTime;
    """),format.raw/*856.5*/("""}"""),format.raw/*856.6*/("""

    """),format.raw/*858.5*/("""void setValue(int v, int a)"""),format.raw/*858.32*/("""{"""),format.raw/*858.33*/("""
        """),format.raw/*859.9*/("""inputVariables[v].status := defined;
        for(i = 0; i &lt; nExternal; i++)"""),format.raw/*860.42*/("""{"""),format.raw/*860.43*/("""
            """),format.raw/*861.13*/("""if(external[currentConfig][i].TrgFMU == id &amp;&amp; external[currentConfig][i].input == v)"""),format.raw/*861.105*/("""{"""),format.raw/*861.106*/("""
                """),format.raw/*862.17*/("""inputVariables[v].time := connectionVariable[external[currentConfig][i].SrcFMU][external[currentConfig][i].output][a].time;
            """),format.raw/*863.13*/("""}"""),format.raw/*863.14*/("""
        """),format.raw/*864.9*/("""}"""),format.raw/*864.10*/("""
    """),format.raw/*865.5*/("""}"""),format.raw/*865.6*/("""

    """),format.raw/*867.5*/("""//Proceed in time - we will start by assuming an FMU can't reject a stepsize
    void doStep(int t)"""),format.raw/*868.23*/("""{"""),format.raw/*868.24*/("""
        """),format.raw/*869.9*/("""//Checking of step is valid
        if(t &gt; stepVariables[id])"""),format.raw/*870.37*/("""{"""),format.raw/*870.38*/("""
        """),format.raw/*871.9*/("""//Step is too big and will not be allowed - t is reset too the biggest allowed step
            t := stepVariables[id];
        """),format.raw/*873.9*/("""}"""),format.raw/*873.10*/("""

        """),format.raw/*875.9*/("""//Take step
        cTime := cTime + t;

        isConsistent := true;

        for(i = 0; i &lt; nInput; i++)"""),format.raw/*880.39*/("""{"""),format.raw/*880.40*/("""
            """),format.raw/*881.13*/("""if(inputVariables[i].time != cTime)"""),format.raw/*881.48*/("""{"""),format.raw/*881.49*/("""
                """),format.raw/*882.17*/("""isConsistent := false;
            """),format.raw/*883.13*/("""}"""),format.raw/*883.14*/("""
        """),format.raw/*884.9*/("""}"""),format.raw/*884.10*/("""

        """),format.raw/*886.9*/("""//Reset outputs accesssed and advance their timestamp
        for(i = 0; i &lt; nOutput; i++)"""),format.raw/*887.40*/("""{"""),format.raw/*887.41*/("""
            """),format.raw/*888.13*/("""//The inputs of the FMUs are inconsistent (not all are at time cTime) - so the FMUs output valid should be set to NaN
            if(isConsistent)"""),format.raw/*889.29*/("""{"""),format.raw/*889.30*/("""
                """),format.raw/*890.17*/("""outputVariables[i].status := undefined;
                outputVariables[i].time := cTime;
            """),format.raw/*892.13*/("""}"""),format.raw/*892.14*/("""else"""),format.raw/*892.18*/("""{"""),format.raw/*892.19*/("""
                """),format.raw/*893.17*/("""outputVariables[i].status := notStable;
                outputVariables[i].time := cTime;
            """),format.raw/*895.13*/("""}"""),format.raw/*895.14*/("""
        """),format.raw/*896.9*/("""}"""),format.raw/*896.10*/("""

        """),format.raw/*898.9*/("""isConsistent := true;

        //Update or return the taken step size
        stepVariables[id] := t;
    """),format.raw/*902.5*/("""}"""),format.raw/*902.6*/("""

    """),format.raw/*904.5*/("""void restoreFMU()"""),format.raw/*904.22*/("""{"""),format.raw/*904.23*/("""
        """),format.raw/*905.9*/("""outputVariables := savedOutputVariables;
        inputVariables := savedInputVariables;
        cTime := savedTime;
    """),format.raw/*908.5*/("""}"""),format.raw/*908.6*/("""

    """),format.raw/*910.5*/("""void saveFMU()"""),format.raw/*910.19*/("""{"""),format.raw/*910.20*/("""
        """),format.raw/*911.9*/("""savedOutputVariables := outputVariables;
        savedInputVariables := inputVariables;
        savedTime := cTime;
        isSaved := true;
    """),format.raw/*915.5*/("""}"""),format.raw/*915.6*/("""

    """),format.raw/*917.5*/("""bool preSet(int v, int a)"""),format.raw/*917.30*/("""{"""),format.raw/*917.31*/("""
        """),format.raw/*918.9*/("""if(checksDisabled)"""),format.raw/*918.27*/("""{"""),format.raw/*918.28*/("""
        """),format.raw/*919.9*/("""return true;
    """),format.raw/*920.5*/("""}"""),format.raw/*920.6*/("""

    """),format.raw/*922.5*/("""//If the connection is reactive the connected variable needs to have a greater than the time of the FMU and be defined
    return (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp;
    inputType[currentConfig][v] == reactive imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time &gt; cTime) &amp;&amp;
    (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp; inputType[currentConfig][v] == delayed
    imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time == cTime);
    """),format.raw/*929.5*/("""}"""),format.raw/*929.6*/("""


    """),format.raw/*932.5*/("""bool preGet(int v)"""),format.raw/*932.23*/("""{"""),format.raw/*932.24*/("""
        """),format.raw/*933.9*/("""if(checksDisabled)"""),format.raw/*933.27*/("""{"""),format.raw/*933.28*/("""
            """),format.raw/*934.13*/("""return true;
        """),format.raw/*935.9*/("""}"""),format.raw/*935.10*/("""

        """),format.raw/*937.9*/("""//All internal connections should be defined at time cTime
        return forall(x:int[0, nInternal-1]) feedthroughInStep[currentConfig][x].FMU == id &amp;&amp; feedthroughInStep[currentConfig][x].output == v
            imply inputVariables[feedthroughInStep[currentConfig][x].input].status == defined &amp;&amp; inputVariables[feedthroughInStep[currentConfig][x].input].time == cTime;
    """),format.raw/*940.5*/("""}"""),format.raw/*940.6*/("""

    """),format.raw/*942.5*/("""bool preDoStep(int t)"""),format.raw/*942.26*/("""{"""),format.raw/*942.27*/("""
        """),format.raw/*943.9*/("""if(checksDisabled)"""),format.raw/*943.27*/("""{"""),format.raw/*943.28*/("""
            """),format.raw/*944.13*/("""return true;
        """),format.raw/*945.9*/("""}"""),format.raw/*945.10*/("""

        """),format.raw/*947.9*/("""//All delayed input ports should be defined at the current time
        //And all reactive inputs ports should be defined at the next time step
        return (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == reactive imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime + t) &amp;&amp;
            (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == delayed imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime);
    """),format.raw/*951.5*/("""}"""),format.raw/*951.6*/("""

        """),format.raw/*953.9*/("""//An FMU can only enter the Simulation mode when all connected FMU variables are defined at time 0
    bool preSimulation()"""),format.raw/*954.25*/("""{"""),format.raw/*954.26*/("""
        """),format.raw/*955.9*/("""return ((forall(x:int[0, MaxNOutputs-1]) outputVariables[x].status == defined &amp;&amp; outputVariables[x].time == 0)
        &amp;&amp; (forall(x:int[0, MaxNInputs-1]) inputVariables[x].status == defined &amp;&amp;
        inputVariables[x].time == 0));
    """),format.raw/*958.5*/("""}"""),format.raw/*958.6*/("""

    """),format.raw/*960.5*/("""bool preSaveFMU()"""),format.raw/*960.22*/("""{"""),format.raw/*960.23*/("""
        """),format.raw/*961.9*/("""//Always possible
        return true;
    """),format.raw/*963.5*/("""}"""),format.raw/*963.6*/("""

    """),format.raw/*965.5*/("""bool preRestoreFMU()"""),format.raw/*965.25*/("""{"""),format.raw/*965.26*/("""
        """),format.raw/*966.9*/("""//Should a requirement be a saved previous FMU?
        return isSaved;
    """),format.raw/*968.5*/("""}"""),format.raw/*968.6*/("""
"""),format.raw/*969.1*/("""</declaration>
    <location id="id28" x="-10285" y="-11662">
        <committed/>
    </location>
    <location id="id29" x="-10387" y="-11305">
        <name x="-10684" y="-11364">Simulation</name>
    </location>
    <init ref="id29"/>
    <transition>
        <source ref="id29"/>
        <target ref="id28"/>
        <label kind="guard" x="-10149" y="-11611">preSaveFMU() &amp;&amp;
            action == save</label>
        <label kind="synchronisation" x="-10081" y="-11526">fmu[id]?</label>
        <label kind="assignment" x="-10089" y="-11492">saveFMU()</label>
        <nail x="-10055" y="-11407"/>
        <nail x="-10055" y="-11551"/>
    </transition>
    <transition>
        <source ref="id29"/>
        <target ref="id28"/>
        <label kind="guard" x="-10276" y="-11543">action == restore &amp;&amp;
            preRestoreFMU()</label>
        <label kind="synchronisation" x="-10251" y="-11500">fmu[id]?</label>
        <label kind="assignment" x="-10276" y="-11466">restoreFMU()</label>
        <nail x="-10234" y="-11415"/>
        <nail x="-10225" y="-11517"/>
    </transition>
    <transition>
        <source ref="id29"/>
        <target ref="id28"/>
        <label kind="guard" x="-10421" y="-11509">action == step &amp;&amp;
            preDoStep(stepsize)</label>
        <label kind="synchronisation" x="-10412" y="-11449">fmu[id]?</label>
        <label kind="assignment" x="-10429" y="-11398">doStep(stepsize)</label>
        <nail x="-10429" y="-11449"/>
        <nail x="-10404" y="-11568"/>
    </transition>
    <transition>
        <source ref="id29"/>
        <target ref="id28"/>
        <label kind="guard" x="-10599" y="-11577">action == get &amp;&amp;
            preGet(var)</label>
        <label kind="synchronisation" x="-10591" y="-11526">fmu[id]?</label>
        <label kind="assignment" x="-10616" y="-11500">getValue(var, commitment)</label>
        <nail x="-10582" y="-11449"/>
        <nail x="-10548" y="-11594"/>
    </transition>
    <transition>
        <source ref="id29"/>
        <target ref="id28"/>
        <label kind="guard" x="-10863" y="-11568">preSet(var, commitment) &amp;&amp;
            action == set</label>
        <label kind="synchronisation" x="-10769" y="-11526">fmu[id]?</label>
        <label kind="assignment" x="-10846" y="-11500">setValue(var, commitment)</label>
        <nail x="-10676" y="-11415"/>
        <nail x="-10608" y="-11645"/>
    </transition>
    <transition>
        <source ref="id28"/>
        <target ref="id29"/>
        <label kind="synchronisation" x="-10226" y="-11679">actionPerformed!</label>
        <label kind="assignment" x="-9971" y="-11560"></label>
        <nail x="-9979" y="-11653"/>
        <nail x="-9979" y="-11313"/>
    </transition>
</template>
    <system>
        // Place template instantiations here.
        MasterA = Interpreter();

        //Max number of tries in the loops is upper bounded by the number of FMUs
        loopS = LoopSolver(nFMU + 1);
        finder = StepFinder(H_max + 1);

        //The arguments to FMU is Id, numbers of outputs, number of inputs, definition of inputTypes
        """),_display_(/*1045.10*/for(fName<- m.fmuNames) yield /*1045.33*/ {_display_(Seq[Any](format.raw/*1045.35*/("""
        """),_display_(/*1046.10*/{fName}),format.raw/*1046.17*/("""_fmu = FMU("""),_display_(/*1046.29*/{fName}),format.raw/*1046.36*/(""", """),_display_(/*1046.39*/{fName}),format.raw/*1046.46*/("""_output, """),_display_(/*1046.56*/{fName}),format.raw/*1046.63*/("""_input, """),_display_(/*1046.72*/{fName}),format.raw/*1046.79*/("""_inputTypes) ;
        """)))}),format.raw/*1047.10*/("""

        """),format.raw/*1049.9*/("""// List one or more processes to be composed into a system.
        system MasterA,
        """),_display_(/*1051.10*/{m.fmuNames.map(fName => s"${fName}_fmu").reduce[String]((a, b) => a + "," + b)}),format.raw/*1051.90*/(""",
        loopS, finder;
    </system>
    <queries>
        <query>
            <formula>A&lt;&gt; MasterA.Terminated
            </formula>
            <comment>
            </comment>
        </query>
    </queries>
</nta>"""))
      }
    }
  }

  def render(m:ModelEncoding): play.twirl.api.XmlFormat.Appendable = apply(m)

  def f:((ModelEncoding) => play.twirl.api.XmlFormat.Appendable) = (m) => apply(m)

  def ref: this.type = this

}


              /*
                  -- GENERATED --
                  SOURCE: src/main/twirl/DynamicCosimUppaalTemplateNoEnabled.scala.xml
                  HASH: 613309f2cf40a95c20bebbd79fa13355107ab094
                  MATRIX: 262->1|656->66|767->85|801->93|2186->1450|2215->1451|2256->1464|2400->1580|2429->1581|2474->1598|2528->1624|2557->1625|2599->1639|2707->1719|2736->1720|2781->1737|2946->1874|2975->1875|3017->1889|3206->2050|3235->2051|3280->2068|3333->2093|3362->2094|3404->2108|3453->2130|3482->2131|3519->2141|3721->2315|3750->2316|3791->2329|3866->2377|3895->2378|3924->2379|4360->2788|4370->2789|4402->2800|4464->2835|4474->2836|4507->2848|4621->2935|4631->2936|4658->2942|4800->3057|4810->3058|4853->3080|4984->3184|4994->3185|5024->3194|5452->3595|5462->3596|5488->3601|5669->3753|5699->3754|5741->3767|6027->4025|6057->4026|6087->4027|6151->4062|6181->4063|6223->4076|6357->4182|6387->4183|6417->4184|6619->4357|6649->4358|6691->4371|6856->4508|6886->4509|6916->4510|6989->4554|7019->4555|7061->4568|7154->4633|7184->4634|7214->4635|9214->6607|9225->6608|9257->6618|9373->6706|9384->6707|9416->6717|9662->6934|9692->6936|9703->6937|9739->6951|9769->6952|9901->7055|9931->7056|9973->7069|10499->7566|10529->7567|10575->7584|10646->7626|10676->7627|10718->7640|10765->7658|10795->7659|10841->7676|10894->7700|10924->7701|10974->7722|11151->7870|11181->7871|11211->7872|11264->7896|11294->7897|11344->7918|11440->7985|11470->7986|11500->7987|11534->7992|11564->7993|11614->8014|11719->8090|11749->8091|11791->8104|11821->8105|11863->8118|11975->8202|12005->8203|12044->8214|12170->8312|12210->8335|12251->8337|12288->8346|12327->8357|12354->8362|12384->8363|12416->8367|12427->8368|12462->8381|12505->8392|12543->8402|12623->8454|12663->8477|12704->8479|12741->8488|12780->8499|12809->8506|12848->8517|12859->8518|12896->8533|12945->8554|12974->8561|13014->8573|13025->8574|13063->8590|13106->8601|13144->8611|13228->8667|13268->8690|13309->8692|13346->8701|13378->8705|13405->8710|13435->8711|13473->8721|13484->8722|13521->8737|13559->8747|13609->8780|13650->8782|13687->8791|13726->8802|13778->8832|13808->8833|13840->8837|13851->8838|13885->8862|13915->8870|13958->8881|13995->8890|14027->8894|14054->8899|14084->8900|14123->8911|14134->8912|14172->8928|14210->8938|14262->8973|14303->8975|14340->8984|14379->8995|14432->9026|14462->9027|14494->9031|14505->9032|14540->9057|14571->9066|14614->9077|14651->9086|14690->9097|14719->9104|14784->9140|14814->9141|14844->9143|14855->9144|14898->9165|14928->9166|14958->9167|15001->9178|15039->9188|15320->9440|15350->9441|15380->9443|15391->9444|15432->9463|15462->9464|15492->9465|15688->9632|15718->9633|15748->9635|15759->9636|15790->9645|15820->9646|15850->9647|15955->9723|15985->9724|16015->9726|16026->9727|16066->9745|16096->9746|16126->9747|16338->9930|16368->9931|16398->9933|16409->9934|16445->9948|16475->9949|16505->9950|16577->9994|16588->9995|16628->10013|16772->10128|16802->10130|16813->10131|16851->10147|16881->10148|17013->10252|17024->10253|17068->10275|17149->10328|17160->10329|17211->10358|17318->10436|17348->10438|17359->10439|17401->10459|17432->10460|17534->10533|17564->10535|17575->10536|17606->10545|17636->10546|17780->10662|17791->10663|17847->10697|17942->10764|17953->10765|18019->10808|18117->10878|18128->10879|18197->10925|18446->11144|18477->11146|18489->11147|18552->11187|18583->11188|18738->11313|18769->11315|18781->11316|18838->11350|18869->11351|19038->11490|19069->11492|19081->11493|19143->11532|19174->11533|19408->11738|19438->11739|19468->11741|19479->11742|19516->11757|19546->11758|19576->11759|20158->12312|20188->12313|20218->12315|20229->12316|20275->12339|20306->12340|20337->12341|20441->12416|20471->12417|20501->12419|20512->12420|20565->12450|20596->12451|20627->12452|20905->12700|20936->12702|20948->12703|21004->12736|21035->12737|21066->12738|21469->13111|21500->13113|21512->13114|21574->13153|21605->13154|21636->13155|21809->13298|21840->13300|21852->13301|21913->13339|21944->13340|21975->13341|22178->13515|22208->13516|22245->13525|22326->13577|22356->13578|22398->13591|22496->13661|22526->13662|22563->13671|22644->13724|22673->13725|22702->13726|26647->17642|26677->17643|26714->17652|26891->17801|26920->17802|26954->17808|27018->17843|27048->17844|27085->17853|27225->17965|27254->17966|27288->17972|27355->18010|27385->18011|27422->18020|27583->18152|27613->18153|27655->18166|27866->18348|27896->18349|27942->18366|28175->18570|28205->18571|28238->18575|28268->18576|28314->18593|28547->18797|28577->18798|28614->18807|28644->18808|28677->18813|28706->18814|28740->18820|28788->18839|28818->18840|28855->18849|29152->19118|29181->19119|29216->19126|29272->19153|29302->19154|29339->19163|29459->19255|29488->19256|29522->19262|29682->19393|29712->19394|29749->19403|30706->20332|30735->20333|30769->20339|30821->20362|30851->20363|30888->20372|30985->20441|31014->20442|31049->20449|31103->20474|31133->20475|31170->20484|31225->20510|31255->20511|31297->20524|31363->20562|31393->20563|31426->20568|31455->20569|31484->20570|37083->26140|37113->26141|37150->26150|37261->26233|37290->26234|37324->26240|37387->26274|37417->26275|37454->26284|37561->26363|37590->26364|37624->26370|37671->26388|37701->26389|37738->26398|37875->26506|37905->26507|37947->26520|38005->26549|38035->26550|38081->26567|38147->26604|38177->26605|38214->26614|38244->26615|38281->26624|38323->26638|38352->26639|38387->26646|38432->26662|38462->26663|38499->26672|38691->26836|38720->26837|38754->26843|38803->26863|38833->26864|38870->26873|38956->26931|38985->26932|39020->26939|39069->26959|39099->26960|39136->26969|39180->26984|39210->26985|39252->26998|39490->27208|39520->27209|39553->27214|39582->27215|39611->27216|44363->31939|44393->31940|44423->31942|44476->31973|44506->31974|44536->31975|44611->32021|44641->32022|44671->32024|44725->32056|44755->32057|44785->32058|45082->32326|45112->32327|45149->32336|45370->32529|45399->32530|45433->32536|45489->32563|45519->32564|45556->32573|45663->32651|45693->32652|45735->32665|45857->32757|45888->32758|45934->32775|46099->32911|46129->32912|46166->32921|46196->32922|46229->32927|46258->32928|46292->32934|46420->33033|46450->33034|46487->33043|46580->33107|46610->33108|46647->33117|46803->33245|46833->33246|46871->33256|47010->33366|47040->33367|47082->33380|47146->33415|47176->33416|47222->33433|47286->33468|47316->33469|47353->33478|47383->33479|47421->33489|47543->33582|47573->33583|47615->33596|47790->33742|47820->33743|47866->33760|47997->33862|48027->33863|48060->33867|48090->33868|48136->33885|48267->33987|48297->33988|48334->33997|48364->33998|48402->34008|48536->34114|48565->34115|48599->34121|48645->34138|48675->34139|48712->34148|48860->34268|48889->34269|48923->34275|48966->34289|48996->34290|49033->34299|49206->34444|49235->34445|49269->34451|49323->34476|49353->34477|49390->34486|49437->34504|49467->34505|49504->34514|49549->34531|49578->34532|49612->34538|50617->35515|50646->35516|50681->35523|50728->35541|50758->35542|50795->35551|50842->35569|50872->35570|50914->35583|50963->35604|50993->35605|51031->35615|51450->36006|51479->36007|51513->36013|51563->36034|51593->36035|51630->36044|51677->36062|51707->36063|51749->36076|51798->36097|51828->36098|51866->36108|52400->36614|52429->36615|52467->36625|52619->36748|52649->36749|52686->36758|52974->37018|53003->37019|53037->37025|53083->37042|53113->37043|53150->37052|53221->37095|53250->37096|53284->37102|53333->37122|53363->37123|53400->37132|53504->37208|53533->37209|53562->37210|56725->40344|56766->40367|56808->40369|56847->40379|56877->40386|56918->40398|56948->40405|56980->40408|57010->40415|57049->40425|57079->40432|57117->40441|57147->40448|57204->40472|57243->40482|57365->40575|57468->40655
                  LINES: 10->1|15->2|20->3|20->3|52->35|52->35|53->36|54->37|54->37|55->38|56->39|56->39|58->41|59->42|59->42|60->43|62->45|62->45|64->47|65->48|65->48|66->49|67->50|67->50|69->52|70->53|70->53|72->55|78->61|78->61|79->62|81->64|81->64|81->64|92->75|92->75|92->75|93->76|93->76|93->76|96->79|96->79|96->79|99->82|99->82|99->82|102->85|102->85|102->85|112->95|112->95|112->95|119->102|119->102|120->103|127->110|127->110|127->110|129->112|129->112|130->113|133->116|133->116|133->116|140->123|140->123|141->124|145->128|145->128|145->128|147->130|147->130|148->131|150->133|150->133|150->133|207->190|207->190|207->190|210->193|210->193|210->193|216->199|216->199|216->199|216->199|216->199|219->202|219->202|220->203|232->215|232->215|233->216|234->217|234->217|235->218|235->218|235->218|236->219|236->219|236->219|237->220|239->222|239->222|239->222|239->222|239->222|240->223|242->225|242->225|242->225|242->225|242->225|243->226|245->228|245->228|246->229|246->229|247->230|249->232|249->232|252->235|254->237|254->237|254->237|255->238|255->238|255->238|255->238|255->238|255->238|255->238|256->239|258->241|259->242|259->242|259->242|260->243|260->243|260->243|260->243|260->243|260->243|261->244|261->244|261->244|261->244|261->244|262->245|264->247|265->248|265->248|265->248|266->249|266->249|266->249|266->249|266->249|266->249|266->249|267->250|267->250|267->250|268->251|268->251|268->251|268->251|268->251|268->251|268->251|268->251|269->252|270->253|270->253|270->253|270->253|270->253|270->253|270->253|271->254|271->254|271->254|272->255|272->255|272->255|272->255|272->255|272->255|272->255|272->255|273->256|274->257|274->257|274->257|274->257|274->257|274->257|274->257|274->257|274->257|274->257|275->258|277->260|279->262|279->262|279->262|279->262|279->262|279->262|279->262|282->265|282->265|282->265|282->265|282->265|282->265|282->265|284->267|284->267|284->267|284->267|284->267|284->267|284->267|287->270|287->270|287->270|287->270|287->270|287->270|287->270|290->273|290->273|290->273|293->276|293->276|293->276|293->276|293->276|296->279|296->279|296->279|297->280|297->280|297->280|299->282|299->282|299->282|299->282|299->282|300->283|300->283|300->283|300->283|300->283|303->286|303->286|303->286|304->287|304->287|304->287|305->288|305->288|305->288|308->291|308->291|308->291|308->291|308->291|309->292|309->292|309->292|309->292|309->292|310->293|310->293|310->293|310->293|310->293|313->296|313->296|313->296|313->296|313->296|313->296|313->296|321->304|321->304|321->304|321->304|321->304|321->304|321->304|322->305|322->305|322->305|322->305|322->305|322->305|322->305|326->309|326->309|326->309|326->309|326->309|326->309|330->313|330->313|330->313|330->313|330->313|330->313|332->315|332->315|332->315|332->315|332->315|332->315|342->325|342->325|343->326|343->326|343->326|344->327|345->328|345->328|346->329|348->331|348->331|349->332|456->439|456->439|457->440|460->443|460->443|462->445|462->445|462->445|463->446|465->448|465->448|467->450|467->450|467->450|468->451|471->454|471->454|472->455|474->457|474->457|475->458|477->460|477->460|477->460|477->460|478->461|480->463|480->463|481->464|481->464|482->465|482->465|484->467|484->467|484->467|485->468|493->476|493->476|496->479|496->479|496->479|497->480|500->483|500->483|502->485|503->486|503->486|504->487|513->496|513->496|515->498|515->498|515->498|516->499|517->500|517->500|520->503|520->503|520->503|521->504|521->504|521->504|522->505|523->506|523->506|524->507|524->507|525->508|677->660|677->660|678->661|680->663|680->663|682->665|682->665|682->665|683->666|685->668|685->668|687->670|687->670|687->670|688->671|691->674|691->674|692->675|692->675|692->675|693->676|694->677|694->677|695->678|695->678|696->679|697->680|697->680|700->683|700->683|700->683|701->684|703->686|703->686|705->688|705->688|705->688|706->689|707->690|707->690|710->693|710->693|710->693|711->694|711->694|711->694|712->695|715->698|715->698|716->699|716->699|717->700|855->838|855->838|855->838|855->838|855->838|855->838|856->839|856->839|856->839|856->839|856->839|856->839|867->850|867->850|868->851|873->856|873->856|875->858|875->858|875->858|876->859|877->860|877->860|878->861|878->861|878->861|879->862|880->863|880->863|881->864|881->864|882->865|882->865|884->867|885->868|885->868|886->869|887->870|887->870|888->871|890->873|890->873|892->875|897->880|897->880|898->881|898->881|898->881|899->882|900->883|900->883|901->884|901->884|903->886|904->887|904->887|905->888|906->889|906->889|907->890|909->892|909->892|909->892|909->892|910->893|912->895|912->895|913->896|913->896|915->898|919->902|919->902|921->904|921->904|921->904|922->905|925->908|925->908|927->910|927->910|927->910|928->911|932->915|932->915|934->917|934->917|934->917|935->918|935->918|935->918|936->919|937->920|937->920|939->922|946->929|946->929|949->932|949->932|949->932|950->933|950->933|950->933|951->934|952->935|952->935|954->937|957->940|957->940|959->942|959->942|959->942|960->943|960->943|960->943|961->944|962->945|962->945|964->947|968->951|968->951|970->953|971->954|971->954|972->955|975->958|975->958|977->960|977->960|977->960|978->961|980->963|980->963|982->965|982->965|982->965|983->966|985->968|985->968|986->969|1062->1045|1062->1045|1062->1045|1063->1046|1063->1046|1063->1046|1063->1046|1063->1046|1063->1046|1063->1046|1063->1046|1063->1046|1063->1046|1064->1047|1066->1049|1068->1051|1068->1051
                  -- GENERATED --
              */
          
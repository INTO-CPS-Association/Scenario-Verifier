
package xml

import _root_.play.twirl.api.JavaScript
import _root_.play.twirl.api.Xml
import _root_.play.twirl.api.Html
import _root_.play.twirl.api.TwirlHelperImports._
import _root_.play.twirl.api.TwirlFeatureImports._
import _root_.play.twirl.api.Txt
/*1.2*/import org.intocps.verification.scenarioverifier.core._

object DynamicCosimUppaalTemplate extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.XmlFormat.Appendable,_root_.play.twirl.api.Format[play.twirl.api.XmlFormat.Appendable]](play.twirl.api.XmlFormat) with _root_.play.twirl.api.Template1[ModelEncoding,play.twirl.api.XmlFormat.Appendable] {

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
        bool shouldChecksBeDisabled()"""),format.raw/*36.38*/("""{"""),format.raw/*36.39*/("""
            """),format.raw/*37.13*/("""//In case a loop is not activated all checks should be
            if(loopActive == -1 &amp;&amp; !stepFinderActive)"""),format.raw/*38.62*/("""{"""),format.raw/*38.63*/("""
                """),format.raw/*39.17*/("""return false;
            """),format.raw/*40.13*/("""}"""),format.raw/*40.14*/("""

            """),format.raw/*42.13*/("""//We are inside a loop is it nested
            if(isLoopNested || isStepNested)"""),format.raw/*43.45*/("""{"""),format.raw/*43.46*/("""
                """),format.raw/*44.17*/("""//Both loops should be on the extraIteration
                return !(isStepExtraIteration &amp;&amp; isLoopExtraIteration);
            """),format.raw/*46.13*/("""}"""),format.raw/*46.14*/("""

            """),format.raw/*48.13*/("""//Not nested - if none of the loops is in the extra iteration we should disable the checks
            if(!isLoopExtraIteration &amp;&amp; !isStepExtraIteration)"""),format.raw/*49.71*/("""{"""),format.raw/*49.72*/("""
                """),format.raw/*50.17*/("""return true;
            """),format.raw/*51.13*/("""}"""),format.raw/*51.14*/("""

            """),format.raw/*53.13*/("""return false;
        """),format.raw/*54.9*/("""}"""),format.raw/*54.10*/("""

        """),format.raw/*56.9*/("""//FMU of a variable
        const int undefined := 0;
        const int defined := 1;
        const int notStable :=-1;

        //FMU of the variable
        typedef struct """),format.raw/*62.24*/("""{"""),format.raw/*62.25*/("""
            """),format.raw/*63.13*/("""int[-1,1] status;
            int time;
        """),format.raw/*65.9*/("""}"""),format.raw/*65.10*/(""" """),format.raw/*65.11*/("""variable;


        //Const assignment types - to future variables or current:
        const int final := 0;
        const int tentative := 1;
        const int noCommitment := -1;

        //***********************************************************************************************************

        //Max number of inputs/outputs any FMU can have - Should be changed
        const int MaxNInputs = """),_display_(/*76.33*/m/*76.34*/.maxNInputs),format.raw/*76.45*/(""";
        const int MaxNOutputs = """),_display_(/*77.34*/m/*77.35*/.maxNOutputs),format.raw/*77.47*/(""";

        //Numbers of FMUs in scenario - Should be changed
        const int nFMU = """),_display_(/*80.27*/m/*80.28*/.nFMUs),format.raw/*80.34*/(""";

        //number of algebraic loops in scenario - Should be changed
        const int nAlgebraicLoopsInStep := """),_display_(/*83.45*/m/*83.46*/.nAlgebraicLoopsInStep),format.raw/*83.68*/(""";

        //Adaptive co-simulation - numbers of different configurations
        const int nConfig := """),_display_(/*86.31*/m/*86.32*/.nConfigs),format.raw/*86.41*/(""";
        //***********************************************************************************************************
        //Do not change

        const int NActions := 14;

        //The number of actions in our system
        const int N := MaxNInputs &gt; MaxNOutputs? MaxNInputs : MaxNOutputs;

        //The maximum step allowed in system - shouldn't be changed
        const int H_max := """),_display_(/*96.29*/m/*96.30*/.Hmax),format.raw/*96.35*/(""";
        const int H := H_max;

        const int noStep := -1;
        const int noFMU := -1;
        const int noLoop := -1;

        typedef struct """),format.raw/*103.24*/("""{"""),format.raw/*103.25*/("""
            """),format.raw/*104.13*/("""int[-1, nFMU] FMU;
            int[-1,NActions] act;
            int[-1,N] portVariable;
            int[-1,H] step_size;
            int[-1,nFMU] relative_step_size;
            int[-1,1] commitment;
            int[-1, nAlgebraicLoopsInStep] loop;
        """),format.raw/*111.9*/("""}"""),format.raw/*111.10*/(""" """),format.raw/*111.11*/("""Operation;

        typedef struct """),format.raw/*113.24*/("""{"""),format.raw/*113.25*/("""
            """),format.raw/*114.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNInputs] input;
            int[-1, MaxNOutputs] output;
        """),format.raw/*117.9*/("""}"""),format.raw/*117.10*/(""" """),format.raw/*117.11*/("""InternalConnection;

        //Types of input ports
        const int delayed := 0;
        const int reactive := 1;
        const int noPort := -1;

        typedef struct """),format.raw/*124.24*/("""{"""),format.raw/*124.25*/("""
            """),format.raw/*125.13*/("""int[0, nFMU] SrcFMU;
            int[0,MaxNOutputs] output;
            int[0,nFMU] TrgFMU;
            int[0,MaxNInputs] input;
        """),format.raw/*129.9*/("""}"""),format.raw/*129.10*/(""" """),format.raw/*129.11*/("""ExternalConnection;

        typedef struct """),format.raw/*131.24*/("""{"""),format.raw/*131.25*/("""
            """),format.raw/*132.13*/("""int[-1,nFMU] FMU;
            int[-1, MaxNOutputs] port;
        """),format.raw/*134.9*/("""}"""),format.raw/*134.10*/(""" """),format.raw/*134.11*/("""FmuOutputPort;


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

        //***********************************************************************************************************
        //Scenario Dependent - Should be changed!

        //Number of internal connections - both init and normal
        const int nInternal := """),_display_(/*194.33*/m/*194.34*/.nInternal),format.raw/*194.44*/(""";

        //Number of external connections in scenario
        const int nExternal := """),_display_(/*197.33*/m/*197.34*/.nExternal),format.raw/*197.44*/(""";

        //The initial of value of h
        int h := H_max;

        //This array is representing the variables of the stepSize that each FMU can take - H_max is the default value
        int stepVariables[nFMU] = """),format.raw/*203.35*/("""{"""),_display_(/*203.37*/m/*203.38*/.stepVariables),format.raw/*203.52*/("""}"""),format.raw/*203.53*/(""";

        //A generic action to pick the next action
        void unpackOperation(Operation operation)"""),format.raw/*206.50*/("""{"""),format.raw/*206.51*/("""
            """),format.raw/*207.13*/("""//action to be performed
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
            if(loopActive == noLoop)"""),format.raw/*219.37*/("""{"""),format.raw/*219.38*/("""
                """),format.raw/*220.17*/("""loopActive := operation.loop;
            """),format.raw/*221.13*/("""}"""),format.raw/*221.14*/("""
            """),format.raw/*222.13*/("""if(action == step)"""),format.raw/*222.31*/("""{"""),format.raw/*222.32*/("""
                """),format.raw/*223.17*/("""if (stepsize == noStep) """),format.raw/*223.41*/("""{"""),format.raw/*223.42*/("""
                    """),format.raw/*224.21*/("""// Step is relative to the fmu referred to by relative_step_size
                    stepsize := stepVariables[relative_step_size];
                """),format.raw/*226.17*/("""}"""),format.raw/*226.18*/(""" """),format.raw/*226.19*/("""else if (stepsize == H) """),format.raw/*226.43*/("""{"""),format.raw/*226.44*/("""
                    """),format.raw/*227.21*/("""// Default step
                    stepsize := h;
                """),format.raw/*229.17*/("""}"""),format.raw/*229.18*/(""" """),format.raw/*229.19*/("""else """),format.raw/*229.24*/("""{"""),format.raw/*229.25*/("""
                    """),format.raw/*230.21*/("""// Absolute step size
                    // Nothing to do.
                """),format.raw/*232.17*/("""}"""),format.raw/*232.18*/("""
            """),format.raw/*233.13*/("""}"""),format.raw/*233.14*/("""
            """),format.raw/*234.13*/("""//Update checkStatus
            checksDisabled = shouldChecksBeDisabled();
        """),format.raw/*236.9*/("""}"""),format.raw/*236.10*/("""


        """),format.raw/*239.9*/("""//Encoding of the scenario
        //Each FMU should have a different ID \in [0, nFMU-1]
        """),_display_(/*241.10*/for(fName<- m.fmuNames) yield /*241.33*/ {_display_(Seq[Any](format.raw/*241.35*/("""
        """),format.raw/*242.9*/("""const int """),_display_(/*242.20*/fName),format.raw/*242.25*/(""" """),format.raw/*242.26*/(""":= """),_display_(/*242.30*/m/*242.31*/.fmuId(fName)),format.raw/*242.44*/(""";
        """)))}),format.raw/*243.10*/("""

        """),format.raw/*245.9*/("""//Number of inputs and outputs of each FMU
        """),_display_(/*246.10*/for(fName<- m.fmuNames) yield /*246.33*/ {_display_(Seq[Any](format.raw/*246.35*/("""
        """),format.raw/*247.9*/("""const int """),_display_(/*247.20*/{fName}),format.raw/*247.27*/("""_input := """),_display_(/*247.38*/m/*247.39*/.nInputs(fName)),format.raw/*247.54*/(""";
        const int """),_display_(/*248.20*/{fName}),format.raw/*248.27*/("""_output := """),_display_(/*248.39*/m/*248.40*/.nOutputs(fName)),format.raw/*248.56*/(""";
        """)))}),format.raw/*249.10*/("""

        """),format.raw/*251.9*/("""//Definition of inputs and outputs of each FMU
        """),_display_(/*252.10*/for(fName<- m.fmuNames) yield /*252.33*/ {_display_(Seq[Any](format.raw/*252.35*/("""
        """),format.raw/*253.9*/("""// """),_display_(/*253.13*/fName),format.raw/*253.18*/(""" """),format.raw/*253.19*/("""inputs - """),_display_(/*253.29*/m/*253.30*/.nInputs(fName)),format.raw/*253.45*/("""
        """),_display_(/*254.10*/for(inName<- m.fmuInNames(fName)) yield /*254.43*/ {_display_(Seq[Any](format.raw/*254.45*/("""
        """),format.raw/*255.9*/("""const int """),_display_(/*255.20*/{m.fmuPortName(fName, inName)}),format.raw/*255.50*/(""" """),format.raw/*255.51*/(""":= """),_display_(/*255.55*/m/*255.56*/.fmuInputEncoding(fName)/*255.80*/(inName)),format.raw/*255.88*/(""";
        """)))}),format.raw/*256.10*/("""
        """),format.raw/*257.9*/("""// """),_display_(/*257.13*/fName),format.raw/*257.18*/(""" """),format.raw/*257.19*/("""outputs - """),_display_(/*257.30*/m/*257.31*/.nOutputs(fName)),format.raw/*257.47*/("""
        """),_display_(/*258.10*/for(outName<- m.fmuOutNames(fName)) yield /*258.45*/ {_display_(Seq[Any](format.raw/*258.47*/("""
        """),format.raw/*259.9*/("""const int """),_display_(/*259.20*/{m.fmuPortName(fName, outName)}),format.raw/*259.51*/(""" """),format.raw/*259.52*/(""":= """),_display_(/*259.56*/m/*259.57*/.fmuOutputEncoding(fName)/*259.82*/(outName)),format.raw/*259.91*/(""";
        """)))}),format.raw/*260.10*/("""
        """),format.raw/*261.9*/("""const int """),_display_(/*261.20*/{fName}),format.raw/*261.27*/("""_inputTypes[nConfig][MaxNInputs] := """),format.raw/*261.63*/("""{"""),format.raw/*261.64*/(""" """),_display_(/*261.66*/m/*261.67*/.fmuInputTypes(fName)),format.raw/*261.88*/(""" """),format.raw/*261.89*/("""}"""),format.raw/*261.90*/(""";
        """)))}),format.raw/*262.10*/("""

        """),format.raw/*264.9*/("""//This array is to keep track of the value of each output port - each output port needs two variables (current and future)
        // and each variable is having two values (defined and time)
        variable connectionVariable[nFMU][MaxNOutputs][2] = """),format.raw/*266.61*/("""{"""),format.raw/*266.62*/(""" """),_display_(/*266.64*/m/*266.65*/.connectionVariable),format.raw/*266.84*/(""" """),format.raw/*266.85*/("""}"""),format.raw/*266.86*/(""";

        //Connections - do not longer contain the type of the input - but it is still a 1:1 mapping
        const ExternalConnection external[nConfig][nExternal] = """),format.raw/*269.65*/("""{"""),format.raw/*269.66*/(""" """),_display_(/*269.68*/m/*269.69*/.external),format.raw/*269.78*/(""" """),format.raw/*269.79*/("""}"""),format.raw/*269.80*/(""";

        const InternalConnection feedthroughInStep[nConfig][nInternal] = """),format.raw/*271.74*/("""{"""),format.raw/*271.75*/(""" """),_display_(/*271.77*/m/*271.78*/.feedthroughInStep),format.raw/*271.96*/(""" """),format.raw/*271.97*/("""}"""),format.raw/*271.98*/(""";

        //The array show if an FMU can reject a step or not - if the FMU can reject a step the value is 1 on the index defined by the fmus
        const bool mayRejectStep[nFMU] = """),format.raw/*274.42*/("""{"""),format.raw/*274.43*/(""" """),_display_(/*274.45*/m/*274.46*/.mayRejectStep),format.raw/*274.60*/(""" """),format.raw/*274.61*/("""}"""),format.raw/*274.62*/(""";


        const int maxStepOperations := """),_display_(/*277.41*/m/*277.42*/.maxStepOperations),format.raw/*277.60*/(""";

        //Numbers of operations in each step
        const int[0,maxStepOperations] nStepOperations[nConfig] := """),format.raw/*280.68*/("""{"""),_display_(/*280.70*/m/*280.71*/.nStepOperations),format.raw/*280.87*/("""}"""),format.raw/*280.88*/(""";

        // Number of operations in the step finding loop
        const int maxFindStepOperations := """),_display_(/*283.45*/m/*283.46*/.maxFindStepOperations),format.raw/*283.68*/(""";
        const int maxFindStepRestoreOperations := """),_display_(/*284.52*/m/*284.53*/.maxFindStepRestoreOperations),format.raw/*284.82*/(""";

        const int[0,maxFindStepOperations] nFindStepOperations[nConfig] := """),format.raw/*286.76*/("""{"""),_display_(/*286.78*/m/*286.79*/.nFindStepOperations),format.raw/*286.99*/("""}"""),format.raw/*286.100*/(""";
        const int[0,maxFindStepRestoreOperations] nRestore[nConfig] := """),format.raw/*287.72*/("""{"""),_display_(/*287.74*/m/*287.75*/.nRestore),format.raw/*287.84*/("""}"""),format.raw/*287.85*/(""";

        // Numbers for algebraic loop operations in step
        const int maxNAlgebraicLoopOperationsInStep := """),_display_(/*290.57*/m/*290.58*/.maxNAlgebraicLoopOperationsInStep),format.raw/*290.92*/(""";
        const int maxNRetryOperationsForAlgebraicLoopsInStep := """),_display_(/*291.66*/m/*291.67*/.maxNRetryOperationsForAlgebraicLoopsInStep),format.raw/*291.110*/(""";
        const int maxNConvergeOperationsForAlgebraicLoopsInStep := """),_display_(/*292.69*/m/*292.70*/.maxNConvergeOperationsForAlgebraicLoopsInStep),format.raw/*292.116*/(""";

        //Numbers of operations to be performed per algebraic loop in step
        const int[0,maxNConvergeOperationsForAlgebraicLoopsInStep] nConvergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*295.142*/("""{"""),_display_(/*295.144*/m/*295.145*/.nConvergencePortsPerAlgebraicLoopInStep),format.raw/*295.185*/("""}"""),format.raw/*295.186*/(""";
        const int[0,maxNAlgebraicLoopOperationsInStep] nOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*296.124*/("""{"""),_display_(/*296.126*/m/*296.127*/.nOperationsPerAlgebraicLoopInStep),format.raw/*296.161*/("""}"""),format.raw/*296.162*/(""";
        const int[0,maxNRetryOperationsForAlgebraicLoopsInStep] nRetryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep] = """),format.raw/*297.138*/("""{"""),_display_(/*297.140*/m/*297.141*/.nRetryOperationsPerAlgebraicLoopInStep),format.raw/*297.180*/("""}"""),format.raw/*297.181*/(""";

        //These operations define what should be performed in the simulation - it is assumed that the operation first loads the fmus
        const Operation stepOperations[nConfig][maxStepOperations] = """),format.raw/*300.70*/("""{"""),format.raw/*300.71*/(""" """),_display_(/*300.73*/m/*300.74*/.stepOperations),format.raw/*300.89*/(""" """),format.raw/*300.90*/("""}"""),format.raw/*300.91*/(""";

        //These are the operations to be performed in order to find the correct step
        //In these operation there is a difference on the third parameter to doStep:
        // H (A step-value greater than the allowed step (Greater than the number of FMUS)) means that we should look at the variable h
        // A stepSize (0:(nFMU-1)) means that the should look at that index in stepVariables use that as the step
        //This is being done inside - findStepAction

        const Operation findStepIteration[nConfig][maxFindStepOperations] = """),format.raw/*308.77*/("""{"""),format.raw/*308.78*/(""" """),_display_(/*308.80*/m/*308.81*/.findStepLoopOperations),format.raw/*308.104*/(""" """),format.raw/*308.105*/("""}"""),format.raw/*308.106*/(""";
        const Operation StepFix[nConfig][maxFindStepRestoreOperations] = """),format.raw/*309.74*/("""{"""),format.raw/*309.75*/(""" """),_display_(/*309.77*/m/*309.78*/.findStepLoopRestoreOperations),format.raw/*309.108*/(""" """),format.raw/*309.109*/("""}"""),format.raw/*309.110*/(""";

        //Possible multiple loops
        //Loop operations are to solve algebraic loops in the co-simulation scenario
        const Operation operationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNAlgebraicLoopOperationsInStep] = """),format.raw/*313.127*/("""{"""),_display_(/*313.129*/m/*313.130*/.operationsPerAlgebraicLoopInStep),format.raw/*313.163*/(""" """),format.raw/*313.164*/("""}"""),format.raw/*313.165*/(""";

        //The converge ports is to mark which variables that needs to be checked in the convergence loop
        //The convention is now to specify the FMU first and the port to denote the variables that should be checked
        const FmuOutputPort convergencePortsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNConvergeOperationsForAlgebraicLoopsInStep] = """),format.raw/*317.149*/("""{"""),_display_(/*317.151*/m/*317.152*/.convergencePortsPerAlgebraicLoopInStep),format.raw/*317.191*/(""" """),format.raw/*317.192*/("""}"""),format.raw/*317.193*/(""";

        const Operation retryOperationsPerAlgebraicLoopInStep[nConfig][nAlgebraicLoopsInStep][maxNRetryOperationsForAlgebraicLoopsInStep] = """),format.raw/*319.141*/("""{"""),_display_(/*319.143*/m/*319.144*/.retryOperationsPerAlgebraicLoopInStep),format.raw/*319.182*/(""" """),format.raw/*319.183*/("""}"""),format.raw/*319.184*/(""";

    </declaration>
<template>
<name>Interpreter</name>
<declaration>
    int inst_pc := 0;
    int cosimstep_pc := 0;
    int n := 0;

    void selectNextCosimStepAction()"""),format.raw/*329.37*/("""{"""),format.raw/*329.38*/("""
        """),format.raw/*330.9*/("""if(cosimstep_pc &lt; nStepOperations[currentConfig])"""),format.raw/*330.61*/("""{"""),format.raw/*330.62*/("""
            """),format.raw/*331.13*/("""unpackOperation(stepOperations[currentConfig][cosimstep_pc]);
        """),format.raw/*332.9*/("""}"""),format.raw/*332.10*/("""
        """),format.raw/*333.9*/("""//Proceed to next action
        cosimstep_pc++;
    """),format.raw/*335.5*/("""}"""),format.raw/*335.6*/("""

    """),format.raw/*337.5*/("""void takeStep(int global_h, int newConfig)"""),format.raw/*337.47*/("""{"""),format.raw/*337.48*/("""
        """),format.raw/*338.9*/("""//h is progression of time
        time := time + h;
        //Reset the loop actions
        cosimstep_pc := 0;
        //reset the global stepsize
        h := global_h;
        //reset n
        n := 0;
        currentConfig := newConfig;
    """),format.raw/*347.5*/("""}"""),format.raw/*347.6*/("""

    """),format.raw/*349.5*/("""void setStepsizeFMU(int fmu, int fmu_step_size)"""),format.raw/*349.52*/("""{"""),format.raw/*349.53*/("""
        """),format.raw/*350.9*/("""if(mayRejectStep[fmu])"""),format.raw/*350.31*/("""{"""),format.raw/*350.32*/("""
            """),format.raw/*351.13*/("""//If an FMU can reject a Step it is maximum step should be updated in each iteration
            stepVariables[fmu] = fmu_step_size;
        """),format.raw/*353.9*/("""}"""),format.raw/*353.10*/("""else"""),format.raw/*353.14*/("""{"""),format.raw/*353.15*/("""
            """),format.raw/*354.13*/("""//If not just set its maximum step to the global step
            stepVariables[fmu] = h;
        """),format.raw/*356.9*/("""}"""),format.raw/*356.10*/("""
        """),format.raw/*357.9*/("""n++;
    """),format.raw/*358.5*/("""}"""),format.raw/*358.6*/("""
"""),format.raw/*359.1*/("""</declaration>
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



    void selectNextLoopAction(int l)"""),format.raw/*468.37*/("""{"""),format.raw/*468.38*/("""
        """),format.raw/*469.9*/("""unpackOperation(operationsPerAlgebraicLoopInStep[currentConfig][l][convergence_pc]);
        //Proceed to next action
        convergence_pc ++;
    """),format.raw/*472.5*/("""}"""),format.raw/*472.6*/("""

    """),format.raw/*474.5*/("""void selectNextRestoreAction(int l)"""),format.raw/*474.40*/("""{"""),format.raw/*474.41*/("""
        """),format.raw/*475.9*/("""unpackOperation(retryOperationsPerAlgebraicLoopInStep[currentConfig][l][restore_pc]);
        restore_pc++;
    """),format.raw/*477.5*/("""}"""),format.raw/*477.6*/("""


    """),format.raw/*480.5*/("""void updateConvergenceVariables(int l)"""),format.raw/*480.43*/("""{"""),format.raw/*480.44*/("""
        """),format.raw/*481.9*/("""int fmu;
        int v;
        int i = 0;
        for(i = 0; i &lt; nConvergencePortsPerAlgebraicLoopInStep[currentConfig][l]; i++)"""),format.raw/*484.90*/("""{"""),format.raw/*484.91*/("""
            """),format.raw/*485.13*/("""fmu = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].FMU;
            v = convergencePortsPerAlgebraicLoopInStep[currentConfig][l][i].port;
            if(isFeedthrough)"""),format.raw/*487.30*/("""{"""),format.raw/*487.31*/("""
                """),format.raw/*488.17*/("""connectionVariable[fmu][v][tentative].status := connectionVariable[fmu][v][final].status;
                connectionVariable[fmu][v][tentative].time := connectionVariable[fmu][v][final].time;
            """),format.raw/*490.13*/("""}"""),format.raw/*490.14*/("""else"""),format.raw/*490.18*/("""{"""),format.raw/*490.19*/("""
                """),format.raw/*491.17*/("""connectionVariable[fmu][v][final].status := connectionVariable[fmu][v][tentative].status;
                connectionVariable[fmu][v][final].time := connectionVariable[fmu][v][tentative].time;
            """),format.raw/*493.13*/("""}"""),format.raw/*493.14*/("""
        """),format.raw/*494.9*/("""}"""),format.raw/*494.10*/("""
    """),format.raw/*495.5*/("""}"""),format.raw/*495.6*/("""

    """),format.raw/*497.5*/("""void loopConverge()"""),format.raw/*497.24*/("""{"""),format.raw/*497.25*/("""
        """),format.raw/*498.9*/("""//Loop not longer active
        loopActive := -1;
        //Loop action counter reset
        convergence_pc := 0;
        //Reset convergence counter
        currentConvergeLoopIteration := 0;
        isLoopExtraIteration:= false;
        isFeedthrough := false;
    """),format.raw/*506.5*/("""}"""),format.raw/*506.6*/("""


    """),format.raw/*509.5*/("""void resetConvergenceloop()"""),format.raw/*509.32*/("""{"""),format.raw/*509.33*/("""
        """),format.raw/*510.9*/("""convergence_pc := 0;
        restore_pc := 0;
        selectNextLoopAction(loopActive);
    """),format.raw/*513.5*/("""}"""),format.raw/*513.6*/("""

    """),format.raw/*515.5*/("""//Convergence will happen when all convergenceVariables have a similar future and current value
    bool convergenceCriteria(int l)"""),format.raw/*516.36*/("""{"""),format.raw/*516.37*/("""
        """),format.raw/*517.9*/("""return forall(x:int[0,maxNConvergeOperationsForAlgebraicLoopsInStep-1])
            convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU != noFMU imply
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].status
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].status
            &amp;&amp;
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][final].time
            ==
            connectionVariable[convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].FMU][convergencePortsPerAlgebraicLoopInStep[currentConfig][l][x].port][tentative].time;
    """),format.raw/*526.5*/("""}"""),format.raw/*526.6*/("""

    """),format.raw/*528.5*/("""bool convergence(int l)"""),format.raw/*528.28*/("""{"""),format.raw/*528.29*/("""
        """),format.raw/*529.9*/("""return (convergenceCriteria(l) &amp;&amp; isLoopExtraIteration);
    """),format.raw/*530.5*/("""}"""),format.raw/*530.6*/("""


    """),format.raw/*533.5*/("""void updateIsExtra(int l)"""),format.raw/*533.30*/("""{"""),format.raw/*533.31*/("""
        """),format.raw/*534.9*/("""if(convergenceCriteria(l))"""),format.raw/*534.35*/("""{"""),format.raw/*534.36*/("""
            """),format.raw/*535.13*/("""isLoopExtraIteration := true;
        """),format.raw/*536.9*/("""}"""),format.raw/*536.10*/("""
    """),format.raw/*537.5*/("""}"""),format.raw/*537.6*/("""
"""),format.raw/*538.1*/("""</declaration>
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

    void selectNextStepFinderAction()"""),format.raw/*690.38*/("""{"""),format.raw/*690.39*/("""
        """),format.raw/*691.9*/("""unpackOperation(findStepIteration[currentConfig][step_pc]);
        step_pc++;
    """),format.raw/*693.5*/("""}"""),format.raw/*693.6*/("""

    """),format.raw/*695.5*/("""void selectNextStepRestoreAction()"""),format.raw/*695.39*/("""{"""),format.raw/*695.40*/("""
        """),format.raw/*696.9*/("""unpackOperation(StepFix[currentConfig][restore_pc]);
        restore_pc++;
    """),format.raw/*698.5*/("""}"""),format.raw/*698.6*/("""

    """),format.raw/*700.5*/("""void findMinStep()"""),format.raw/*700.23*/("""{"""),format.raw/*700.24*/("""
        """),format.raw/*701.9*/("""//Maximum step size allowed
        int min = nFMU;
        int j := 0;
        for(j = 0; j &lt; nFMU; j++)"""),format.raw/*704.37*/("""{"""),format.raw/*704.38*/("""
            """),format.raw/*705.13*/("""if(stepVariables[j] &lt; min)"""),format.raw/*705.42*/("""{"""),format.raw/*705.43*/("""
                """),format.raw/*706.17*/("""min := stepVariables[j];
            """),format.raw/*707.13*/("""}"""),format.raw/*707.14*/("""
        """),format.raw/*708.9*/("""}"""),format.raw/*708.10*/("""
        """),format.raw/*709.9*/("""h := min;
    """),format.raw/*710.5*/("""}"""),format.raw/*710.6*/("""


    """),format.raw/*713.5*/("""bool stepFound()"""),format.raw/*713.21*/("""{"""),format.raw/*713.22*/("""
        """),format.raw/*714.9*/("""//All FMU that may reject a step should be able to take the same step - h
        return forall(x:int[0, nFMU-1]) mayRejectStep[x] imply stepVariables[x] == h;
    """),format.raw/*716.5*/("""}"""),format.raw/*716.6*/("""

    """),format.raw/*718.5*/("""bool loopConverged()"""),format.raw/*718.25*/("""{"""),format.raw/*718.26*/("""
        """),format.raw/*719.9*/("""return (stepFound() &amp;&amp; isStepExtraIteration);
    """),format.raw/*720.5*/("""}"""),format.raw/*720.6*/("""


    """),format.raw/*723.5*/("""void updateIsExtra()"""),format.raw/*723.25*/("""{"""),format.raw/*723.26*/("""
        """),format.raw/*724.9*/("""if(stepFound())"""),format.raw/*724.24*/("""{"""),format.raw/*724.25*/("""
            """),format.raw/*725.13*/("""isStepExtraIteration := true;
            //Reset numbers of tries to 0 - This is to avoid problems with the maximum number of tries and not to active the nested checks
            numbersOfTries := 0;
        """),format.raw/*728.9*/("""}"""),format.raw/*728.10*/("""
    """),format.raw/*729.5*/("""}"""),format.raw/*729.6*/("""
"""),format.raw/*730.1*/("""</declaration>
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
    variable inputVariables[MaxNInputs] = """),format.raw/*868.43*/("""{"""),format.raw/*868.44*/(""" """),_display_(/*868.46*/{m.variableArray(m.maxNInputs)}),format.raw/*868.77*/(""" """),format.raw/*868.78*/("""}"""),format.raw/*868.79*/(""";
    variable outputVariables[MaxNOutputs] = """),format.raw/*869.45*/("""{"""),format.raw/*869.46*/(""" """),_display_(/*869.48*/{m.variableArray(m.maxNOutputs)}),format.raw/*869.80*/(""" """),format.raw/*869.81*/("""}"""),format.raw/*869.82*/(""";
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
    bool getEnabled[MaxNOutputs] := """),format.raw/*882.37*/("""{"""),format.raw/*882.38*/(""" """),_display_(/*882.40*/m/*882.41*/.getEnabled),format.raw/*882.52*/(""" """),format.raw/*882.53*/("""}"""),format.raw/*882.54*/(""";
    bool setEnabled[MaxNInputs] := """),format.raw/*883.36*/("""{"""),format.raw/*883.37*/(""" """),_display_(/*883.39*/m/*883.40*/.setEnabled),format.raw/*883.51*/(""" """),format.raw/*883.52*/("""}"""),format.raw/*883.53*/(""";

    void getValue(int v, int a)"""),format.raw/*885.32*/("""{"""),format.raw/*885.33*/("""
        """),format.raw/*886.9*/("""outputVariables[v].status := defined;
        outputVariables[v].time := cTime;

        connectionVariable[id][v][a].status := defined;
        connectionVariable[id][v][a].time := cTime;
    """),format.raw/*891.5*/("""}"""),format.raw/*891.6*/("""

    """),format.raw/*893.5*/("""void setValue(int v, int a)"""),format.raw/*893.32*/("""{"""),format.raw/*893.33*/("""
        """),format.raw/*894.9*/("""inputVariables[v].status := defined;
        for(i = 0; i &lt; nExternal; i++)"""),format.raw/*895.42*/("""{"""),format.raw/*895.43*/("""
            """),format.raw/*896.13*/("""if(external[currentConfig][i].TrgFMU == id &amp;&amp; external[currentConfig][i].input == v)"""),format.raw/*896.105*/("""{"""),format.raw/*896.106*/("""
                """),format.raw/*897.17*/("""inputVariables[v].time := connectionVariable[external[currentConfig][i].SrcFMU][external[currentConfig][i].output][a].time;
            """),format.raw/*898.13*/("""}"""),format.raw/*898.14*/("""
        """),format.raw/*899.9*/("""}"""),format.raw/*899.10*/("""
    """),format.raw/*900.5*/("""}"""),format.raw/*900.6*/("""

    """),format.raw/*902.5*/("""//Proceed in time - we will start by assuming an FMU can't reject a stepsize
    void doStep(int t)"""),format.raw/*903.23*/("""{"""),format.raw/*903.24*/("""
        """),format.raw/*904.9*/("""//Checking of step is valid
        if(t &gt; stepVariables[id])"""),format.raw/*905.37*/("""{"""),format.raw/*905.38*/("""
        """),format.raw/*906.9*/("""//Step is too big and will not be allowed - t is reset too the biggest allowed step
            t := stepVariables[id];
        """),format.raw/*908.9*/("""}"""),format.raw/*908.10*/("""

        """),format.raw/*910.9*/("""//Take step
        cTime := cTime + t;

        isConsistent := true;

        for(i = 0; i &lt; nInput; i++)"""),format.raw/*915.39*/("""{"""),format.raw/*915.40*/("""
            """),format.raw/*916.13*/("""if(inputVariables[i].time != cTime)"""),format.raw/*916.48*/("""{"""),format.raw/*916.49*/("""
                """),format.raw/*917.17*/("""isConsistent := false;
            """),format.raw/*918.13*/("""}"""),format.raw/*918.14*/("""
        """),format.raw/*919.9*/("""}"""),format.raw/*919.10*/("""

        """),format.raw/*921.9*/("""//Reset outputs accesssed and advance their timestamp
        for(i = 0; i &lt; nOutput; i++)"""),format.raw/*922.40*/("""{"""),format.raw/*922.41*/("""
            """),format.raw/*923.13*/("""//The inputs of the FMUs are inconsistent (not all are at time cTime) - so the FMUs output valid should be set to NaN
            if(isConsistent)"""),format.raw/*924.29*/("""{"""),format.raw/*924.30*/("""
                """),format.raw/*925.17*/("""outputVariables[i].status := undefined;
                outputVariables[i].time := cTime;
            """),format.raw/*927.13*/("""}"""),format.raw/*927.14*/("""else"""),format.raw/*927.18*/("""{"""),format.raw/*927.19*/("""
                """),format.raw/*928.17*/("""outputVariables[i].status := notStable;
                outputVariables[i].time := cTime;
            """),format.raw/*930.13*/("""}"""),format.raw/*930.14*/("""
        """),format.raw/*931.9*/("""}"""),format.raw/*931.10*/("""

        """),format.raw/*933.9*/("""isConsistent := true;

        //Update or return the taken step size
        stepVariables[id] := t;
    """),format.raw/*937.5*/("""}"""),format.raw/*937.6*/("""

    """),format.raw/*939.5*/("""void restoreFMU()"""),format.raw/*939.22*/("""{"""),format.raw/*939.23*/("""
        """),format.raw/*940.9*/("""outputVariables := savedOutputVariables;
        inputVariables := savedInputVariables;
        cTime := savedTime;
    """),format.raw/*943.5*/("""}"""),format.raw/*943.6*/("""

    """),format.raw/*945.5*/("""void saveFMU()"""),format.raw/*945.19*/("""{"""),format.raw/*945.20*/("""
        """),format.raw/*946.9*/("""savedOutputVariables := outputVariables;
        savedInputVariables := inputVariables;
        savedTime := cTime;
        isSaved := true;
    """),format.raw/*950.5*/("""}"""),format.raw/*950.6*/("""

    """),format.raw/*952.5*/("""bool preSet(int v, int a)"""),format.raw/*952.30*/("""{"""),format.raw/*952.31*/("""
        """),format.raw/*953.9*/("""if(checksDisabled)"""),format.raw/*953.27*/("""{"""),format.raw/*953.28*/("""
        """),format.raw/*954.9*/("""return true;
    """),format.raw/*955.5*/("""}"""),format.raw/*955.6*/("""

    """),format.raw/*957.5*/("""//If the connection is reactive the connected variable needs to have a greater than the time of the FMU and be defined
    return (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp;
    inputType[currentConfig][v] == reactive imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time &gt; cTime) &amp;&amp;
    (forall(x:int[0, nExternal-1]) external[currentConfig][x].TrgFMU == id &amp;&amp; external[currentConfig][x].input == v &amp;&amp; inputType[currentConfig][v] == delayed
    imply connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].status == defined &amp;&amp;
    connectionVariable[external[currentConfig][x].SrcFMU][external[currentConfig][x].output][a].time == cTime);
    """),format.raw/*964.5*/("""}"""),format.raw/*964.6*/("""


    """),format.raw/*967.5*/("""bool preGet(int v)"""),format.raw/*967.23*/("""{"""),format.raw/*967.24*/("""
        """),format.raw/*968.9*/("""if(checksDisabled)"""),format.raw/*968.27*/("""{"""),format.raw/*968.28*/("""
            """),format.raw/*969.13*/("""return true;
        """),format.raw/*970.9*/("""}"""),format.raw/*970.10*/("""

        """),format.raw/*972.9*/("""//All internal connections should be defined at time cTime
        return forall(x:int[0, nInternal-1]) feedthroughInStep[currentConfig][x].FMU == id &amp;&amp; feedthroughInStep[currentConfig][x].output == v
            imply inputVariables[feedthroughInStep[currentConfig][x].input].status == defined &amp;&amp; inputVariables[feedthroughInStep[currentConfig][x].input].time == cTime;
    """),format.raw/*975.5*/("""}"""),format.raw/*975.6*/("""

    """),format.raw/*977.5*/("""bool preDoStep(int t)"""),format.raw/*977.26*/("""{"""),format.raw/*977.27*/("""
        """),format.raw/*978.9*/("""if(checksDisabled)"""),format.raw/*978.27*/("""{"""),format.raw/*978.28*/("""
            """),format.raw/*979.13*/("""return true;
        """),format.raw/*980.9*/("""}"""),format.raw/*980.10*/("""

        """),format.raw/*982.9*/("""//All delayed input ports should be defined at the current time
        //And all reactive inputs ports should be defined at the next time step
        return (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == reactive imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime + t) &amp;&amp;
            (forall(x:int[0, MaxNInputs-1]) inputType[currentConfig][x] == delayed imply inputVariables[x].status == defined &amp;&amp; inputVariables[x].time == cTime);
    """),format.raw/*986.5*/("""}"""),format.raw/*986.6*/("""

        """),format.raw/*988.9*/("""//An FMU can only enter the Simulation mode when all connected FMU variables are defined at time 0
    bool preSimulation()"""),format.raw/*989.25*/("""{"""),format.raw/*989.26*/("""
        """),format.raw/*990.9*/("""return ((forall(x:int[0, MaxNOutputs-1]) outputVariables[x].status == defined &amp;&amp; outputVariables[x].time == 0)
        &amp;&amp; (forall(x:int[0, MaxNInputs-1]) inputVariables[x].status == defined &amp;&amp;
        inputVariables[x].time == 0));
    """),format.raw/*993.5*/("""}"""),format.raw/*993.6*/("""

    """),format.raw/*995.5*/("""bool preSaveFMU()"""),format.raw/*995.22*/("""{"""),format.raw/*995.23*/("""
        """),format.raw/*996.9*/("""//Always possible
        return true;
    """),format.raw/*998.5*/("""}"""),format.raw/*998.6*/("""

    """),format.raw/*1000.5*/("""bool preRestoreFMU()"""),format.raw/*1000.25*/("""{"""),format.raw/*1000.26*/("""
        """),format.raw/*1001.9*/("""//Should a requirement be a saved previous FMU?
        return isSaved;
    """),format.raw/*1003.5*/("""}"""),format.raw/*1003.6*/("""

    """),format.raw/*1005.5*/("""void updateEnableActions()"""),format.raw/*1005.31*/("""{"""),format.raw/*1005.32*/("""
        """),format.raw/*1006.9*/("""for(i = 0; i &lt; nInput; i++)"""),format.raw/*1006.39*/("""{"""),format.raw/*1006.40*/("""
            """),format.raw/*1007.13*/("""setEnabled[i] := preSet(i, final);
        """),format.raw/*1008.9*/("""}"""),format.raw/*1008.10*/("""
        """),format.raw/*1009.9*/("""for(i := 0; i &lt; nOutput; i++)"""),format.raw/*1009.41*/("""{"""),format.raw/*1009.42*/("""
            """),format.raw/*1010.13*/("""getEnabled[i] := preGet(i);
        """),format.raw/*1011.9*/("""}"""),format.raw/*1011.10*/("""
        """),format.raw/*1012.9*/("""stepEnabled := preDoStep(h);
    """),format.raw/*1013.5*/("""}"""),format.raw/*1013.6*/("""
"""),format.raw/*1014.1*/("""</declaration>
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
        <label kind="assignment" x="-9971" y="-11560">updateEnableActions()</label>
        <nail x="-9979" y="-11653"/>
        <nail x="-9979" y="-11313"/>
    </transition>
    <transition>
        <source ref="id29"/>
        <target ref="id29"/>
        <label kind="synchronisation" x="-10633" y="-11203">actionPerformed?</label>
        <label kind="assignment" x="-10659" y="-11177">updateEnableActions()</label>
        <nail x="-10438" y="-11143"/>
        <nail x="-10557" y="-11237"/>
    </transition>
</template>
    <system>
        // Place template instantiations here.
        MasterA = Interpreter();

        //Max number of tries in the loops is upper bounded by the number of FMUs
        loopS = LoopSolver(nFMU + 1);
        finder = StepFinder(H_max + 1);

        //The arguments to FMU is Id, numbers of outputs, number of inputs, definition of inputTypes
        """),_display_(/*1098.10*/for(fName<- m.fmuNames) yield /*1098.33*/ {_display_(Seq[Any](format.raw/*1098.35*/("""
        """),_display_(/*1099.10*/{fName}),format.raw/*1099.17*/("""_fmu = FMU("""),_display_(/*1099.29*/{fName}),format.raw/*1099.36*/(""", """),_display_(/*1099.39*/{fName}),format.raw/*1099.46*/("""_output, """),_display_(/*1099.56*/{fName}),format.raw/*1099.63*/("""_input, """),_display_(/*1099.72*/{fName}),format.raw/*1099.79*/("""_inputTypes) ;
        """)))}),format.raw/*1100.10*/("""

        """),format.raw/*1102.9*/("""// List one or more processes to be composed into a system.
        system MasterA,
        """),_display_(/*1104.10*/{m.fmuNames.map(fName => s"${fName}_fmu").reduce[String]((a, b) => a + "," + b)}),format.raw/*1104.90*/(""",
        loopS, finder;
    </system>
    <queries>
        <query>
            <formula>E&lt;&gt; MasterA.Terminated
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
                  SOURCE: src/main/twirl/DynamicCosimUppaalTemplate.scala.xml
                  HASH: 4a12db81fcae0d1624057a4b9689fea1bfb8f6d5
                  MATRIX: 262->1|647->66|758->85|792->93|2178->1451|2207->1452|2248->1465|2392->1581|2421->1582|2466->1599|2520->1625|2549->1626|2591->1640|2699->1720|2728->1721|2773->1738|2938->1875|2967->1876|3009->1890|3198->2051|3227->2052|3272->2069|3325->2094|3354->2095|3396->2109|3445->2131|3474->2132|3511->2142|3713->2316|3742->2317|3783->2330|3858->2378|3887->2379|3916->2380|4352->2789|4362->2790|4394->2801|4456->2836|4466->2837|4499->2849|4613->2936|4623->2937|4650->2943|4792->3058|4802->3059|4845->3081|4976->3185|4986->3186|5016->3195|5444->3596|5454->3597|5480->3602|5661->3754|5691->3755|5733->3768|6019->4026|6049->4027|6079->4028|6143->4063|6173->4064|6215->4077|6349->4183|6379->4184|6409->4185|6611->4358|6641->4359|6683->4372|6848->4509|6878->4510|6908->4511|6981->4555|7011->4556|7053->4569|7146->4634|7176->4635|7206->4636|9293->6695|9304->6696|9336->6706|9452->6794|9463->6795|9495->6805|9741->7022|9771->7024|9782->7025|9818->7039|9848->7040|9980->7143|10010->7144|10052->7157|10578->7654|10608->7655|10654->7672|10725->7714|10755->7715|10797->7728|10844->7746|10874->7747|10920->7764|10973->7788|11003->7789|11053->7810|11230->7958|11260->7959|11290->7960|11343->7984|11373->7985|11423->8006|11519->8073|11549->8074|11579->8075|11613->8080|11643->8081|11693->8102|11798->8178|11828->8179|11870->8192|11900->8193|11942->8206|12054->8290|12084->8291|12123->8302|12249->8400|12289->8423|12330->8425|12367->8434|12406->8445|12433->8450|12463->8451|12495->8455|12506->8456|12541->8469|12584->8480|12622->8490|12702->8542|12742->8565|12783->8567|12820->8576|12859->8587|12888->8594|12927->8605|12938->8606|12975->8621|13024->8642|13053->8649|13093->8661|13104->8662|13142->8678|13185->8689|13223->8699|13307->8755|13347->8778|13388->8780|13425->8789|13457->8793|13484->8798|13514->8799|13552->8809|13563->8810|13600->8825|13638->8835|13688->8868|13729->8870|13766->8879|13805->8890|13857->8920|13887->8921|13919->8925|13930->8926|13964->8950|13994->8958|14037->8969|14074->8978|14106->8982|14133->8987|14163->8988|14202->8999|14213->9000|14251->9016|14289->9026|14341->9061|14382->9063|14419->9072|14458->9083|14511->9114|14541->9115|14573->9119|14584->9120|14619->9145|14650->9154|14693->9165|14730->9174|14769->9185|14798->9192|14863->9228|14893->9229|14923->9231|14934->9232|14977->9253|15007->9254|15037->9255|15080->9266|15118->9276|15399->9528|15429->9529|15459->9531|15470->9532|15511->9551|15541->9552|15571->9553|15767->9720|15797->9721|15827->9723|15838->9724|15869->9733|15899->9734|15929->9735|16034->9811|16064->9812|16094->9814|16105->9815|16145->9833|16175->9834|16205->9835|16417->10018|16447->10019|16477->10021|16488->10022|16524->10036|16554->10037|16584->10038|16656->10082|16667->10083|16707->10101|16851->10216|16881->10218|16892->10219|16930->10235|16960->10236|17092->10340|17103->10341|17147->10363|17228->10416|17239->10417|17290->10446|17397->10524|17427->10526|17438->10527|17480->10547|17511->10548|17613->10621|17643->10623|17654->10624|17685->10633|17715->10634|17859->10750|17870->10751|17926->10785|18021->10852|18032->10853|18098->10896|18196->10966|18207->10967|18276->11013|18525->11232|18556->11234|18568->11235|18631->11275|18662->11276|18817->11401|18848->11403|18860->11404|18917->11438|18948->11439|19117->11578|19148->11580|19160->11581|19222->11620|19253->11621|19487->11826|19517->11827|19547->11829|19558->11830|19595->11845|19625->11846|19655->11847|20237->12400|20267->12401|20297->12403|20308->12404|20354->12427|20385->12428|20416->12429|20520->12504|20550->12505|20580->12507|20591->12508|20644->12538|20675->12539|20706->12540|20984->12788|21015->12790|21027->12791|21083->12824|21114->12825|21145->12826|21548->13199|21579->13201|21591->13202|21653->13241|21684->13242|21715->13243|21888->13386|21919->13388|21931->13389|21992->13427|22023->13428|22054->13429|22257->13603|22287->13604|22324->13613|22405->13665|22435->13666|22477->13679|22575->13749|22605->13750|22642->13759|22723->13812|22752->13813|22786->13819|22857->13861|22887->13862|22924->13871|23198->14117|23227->14118|23261->14124|23337->14171|23367->14172|23404->14181|23455->14203|23485->14204|23527->14217|23696->14358|23726->14359|23759->14363|23789->14364|23831->14377|23957->14475|23987->14476|24024->14485|24061->14494|24090->14495|24119->14496|28066->18414|28096->18415|28133->18424|28310->18573|28339->18574|28373->18580|28437->18615|28467->18616|28504->18625|28644->18737|28673->18738|28708->18745|28775->18783|28805->18784|28842->18793|29003->18925|29033->18926|29075->18939|29286->19121|29316->19122|29362->19139|29595->19343|29625->19344|29658->19348|29688->19349|29734->19366|29967->19570|29997->19571|30034->19580|30064->19581|30097->19586|30126->19587|30160->19593|30208->19612|30238->19613|30275->19622|30572->19891|30601->19892|30636->19899|30692->19926|30722->19927|30759->19936|30879->20028|30908->20029|30942->20035|31102->20166|31132->20167|31169->20176|32126->21105|32155->21106|32189->21112|32241->21135|32271->21136|32308->21145|32405->21214|32434->21215|32469->21222|32523->21247|32553->21248|32590->21257|32645->21283|32675->21284|32717->21297|32783->21335|32813->21336|32846->21341|32875->21342|32904->21343|38503->26913|38533->26914|38570->26923|38681->27006|38710->27007|38744->27013|38807->27047|38837->27048|38874->27057|38981->27136|39010->27137|39044->27143|39091->27161|39121->27162|39158->27171|39295->27279|39325->27280|39367->27293|39425->27322|39455->27323|39501->27340|39567->27377|39597->27378|39634->27387|39664->27388|39701->27397|39743->27411|39772->27412|39807->27419|39852->27435|39882->27436|39919->27445|40111->27609|40140->27610|40174->27616|40223->27636|40253->27637|40290->27646|40376->27704|40405->27705|40440->27712|40489->27732|40519->27733|40556->27742|40600->27757|40630->27758|40672->27771|40910->27981|40940->27982|40973->27987|41002->27988|41031->27989|45783->32712|45813->32713|45843->32715|45896->32746|45926->32747|45956->32748|46031->32794|46061->32795|46091->32797|46145->32829|46175->32830|46205->32831|46570->33167|46600->33168|46630->33170|46641->33171|46674->33182|46704->33183|46734->33184|46800->33221|46830->33222|46860->33224|46871->33225|46904->33236|46934->33237|46964->33238|47027->33272|47057->33273|47094->33282|47315->33475|47344->33476|47378->33482|47434->33509|47464->33510|47501->33519|47608->33597|47638->33598|47680->33611|47802->33703|47833->33704|47879->33721|48044->33857|48074->33858|48111->33867|48141->33868|48174->33873|48203->33874|48237->33880|48365->33979|48395->33980|48432->33989|48525->34053|48555->34054|48592->34063|48748->34191|48778->34192|48816->34202|48955->34312|48985->34313|49027->34326|49091->34361|49121->34362|49167->34379|49231->34414|49261->34415|49298->34424|49328->34425|49366->34435|49488->34528|49518->34529|49560->34542|49735->34688|49765->34689|49811->34706|49942->34808|49972->34809|50005->34813|50035->34814|50081->34831|50212->34933|50242->34934|50279->34943|50309->34944|50347->34954|50481->35060|50510->35061|50544->35067|50590->35084|50620->35085|50657->35094|50805->35214|50834->35215|50868->35221|50911->35235|50941->35236|50978->35245|51151->35390|51180->35391|51214->35397|51268->35422|51298->35423|51335->35432|51382->35450|51412->35451|51449->35460|51494->35477|51523->35478|51557->35484|52562->36461|52591->36462|52626->36469|52673->36487|52703->36488|52740->36497|52787->36515|52817->36516|52859->36529|52908->36550|52938->36551|52976->36561|53395->36952|53424->36953|53458->36959|53508->36980|53538->36981|53575->36990|53622->37008|53652->37009|53694->37022|53743->37043|53773->37044|53811->37054|54345->37560|54374->37561|54412->37571|54564->37694|54594->37695|54631->37704|54919->37964|54948->37965|54982->37971|55028->37988|55058->37989|55095->37998|55166->38041|55195->38042|55230->38048|55280->38068|55311->38069|55349->38078|55454->38154|55484->38155|55519->38161|55575->38187|55606->38188|55644->38197|55704->38227|55735->38228|55778->38241|55850->38284|55881->38285|55919->38294|55981->38326|56012->38327|56055->38340|56120->38376|56151->38377|56189->38386|56251->38419|56281->38420|56311->38421|59834->41915|59875->41938|59917->41940|59956->41950|59986->41957|60027->41969|60057->41976|60089->41979|60119->41986|60158->41996|60188->42003|60226->42012|60256->42019|60313->42043|60352->42053|60474->42146|60577->42226
                  LINES: 10->1|15->2|20->3|20->3|53->36|53->36|54->37|55->38|55->38|56->39|57->40|57->40|59->42|60->43|60->43|61->44|63->46|63->46|65->48|66->49|66->49|67->50|68->51|68->51|70->53|71->54|71->54|73->56|79->62|79->62|80->63|82->65|82->65|82->65|93->76|93->76|93->76|94->77|94->77|94->77|97->80|97->80|97->80|100->83|100->83|100->83|103->86|103->86|103->86|113->96|113->96|113->96|120->103|120->103|121->104|128->111|128->111|128->111|130->113|130->113|131->114|134->117|134->117|134->117|141->124|141->124|142->125|146->129|146->129|146->129|148->131|148->131|149->132|151->134|151->134|151->134|211->194|211->194|211->194|214->197|214->197|214->197|220->203|220->203|220->203|220->203|220->203|223->206|223->206|224->207|236->219|236->219|237->220|238->221|238->221|239->222|239->222|239->222|240->223|240->223|240->223|241->224|243->226|243->226|243->226|243->226|243->226|244->227|246->229|246->229|246->229|246->229|246->229|247->230|249->232|249->232|250->233|250->233|251->234|253->236|253->236|256->239|258->241|258->241|258->241|259->242|259->242|259->242|259->242|259->242|259->242|259->242|260->243|262->245|263->246|263->246|263->246|264->247|264->247|264->247|264->247|264->247|264->247|265->248|265->248|265->248|265->248|265->248|266->249|268->251|269->252|269->252|269->252|270->253|270->253|270->253|270->253|270->253|270->253|270->253|271->254|271->254|271->254|272->255|272->255|272->255|272->255|272->255|272->255|272->255|272->255|273->256|274->257|274->257|274->257|274->257|274->257|274->257|274->257|275->258|275->258|275->258|276->259|276->259|276->259|276->259|276->259|276->259|276->259|276->259|277->260|278->261|278->261|278->261|278->261|278->261|278->261|278->261|278->261|278->261|278->261|279->262|281->264|283->266|283->266|283->266|283->266|283->266|283->266|283->266|286->269|286->269|286->269|286->269|286->269|286->269|286->269|288->271|288->271|288->271|288->271|288->271|288->271|288->271|291->274|291->274|291->274|291->274|291->274|291->274|291->274|294->277|294->277|294->277|297->280|297->280|297->280|297->280|297->280|300->283|300->283|300->283|301->284|301->284|301->284|303->286|303->286|303->286|303->286|303->286|304->287|304->287|304->287|304->287|304->287|307->290|307->290|307->290|308->291|308->291|308->291|309->292|309->292|309->292|312->295|312->295|312->295|312->295|312->295|313->296|313->296|313->296|313->296|313->296|314->297|314->297|314->297|314->297|314->297|317->300|317->300|317->300|317->300|317->300|317->300|317->300|325->308|325->308|325->308|325->308|325->308|325->308|325->308|326->309|326->309|326->309|326->309|326->309|326->309|326->309|330->313|330->313|330->313|330->313|330->313|330->313|334->317|334->317|334->317|334->317|334->317|334->317|336->319|336->319|336->319|336->319|336->319|336->319|346->329|346->329|347->330|347->330|347->330|348->331|349->332|349->332|350->333|352->335|352->335|354->337|354->337|354->337|355->338|364->347|364->347|366->349|366->349|366->349|367->350|367->350|367->350|368->351|370->353|370->353|370->353|370->353|371->354|373->356|373->356|374->357|375->358|375->358|376->359|485->468|485->468|486->469|489->472|489->472|491->474|491->474|491->474|492->475|494->477|494->477|497->480|497->480|497->480|498->481|501->484|501->484|502->485|504->487|504->487|505->488|507->490|507->490|507->490|507->490|508->491|510->493|510->493|511->494|511->494|512->495|512->495|514->497|514->497|514->497|515->498|523->506|523->506|526->509|526->509|526->509|527->510|530->513|530->513|532->515|533->516|533->516|534->517|543->526|543->526|545->528|545->528|545->528|546->529|547->530|547->530|550->533|550->533|550->533|551->534|551->534|551->534|552->535|553->536|553->536|554->537|554->537|555->538|707->690|707->690|708->691|710->693|710->693|712->695|712->695|712->695|713->696|715->698|715->698|717->700|717->700|717->700|718->701|721->704|721->704|722->705|722->705|722->705|723->706|724->707|724->707|725->708|725->708|726->709|727->710|727->710|730->713|730->713|730->713|731->714|733->716|733->716|735->718|735->718|735->718|736->719|737->720|737->720|740->723|740->723|740->723|741->724|741->724|741->724|742->725|745->728|745->728|746->729|746->729|747->730|885->868|885->868|885->868|885->868|885->868|885->868|886->869|886->869|886->869|886->869|886->869|886->869|899->882|899->882|899->882|899->882|899->882|899->882|899->882|900->883|900->883|900->883|900->883|900->883|900->883|900->883|902->885|902->885|903->886|908->891|908->891|910->893|910->893|910->893|911->894|912->895|912->895|913->896|913->896|913->896|914->897|915->898|915->898|916->899|916->899|917->900|917->900|919->902|920->903|920->903|921->904|922->905|922->905|923->906|925->908|925->908|927->910|932->915|932->915|933->916|933->916|933->916|934->917|935->918|935->918|936->919|936->919|938->921|939->922|939->922|940->923|941->924|941->924|942->925|944->927|944->927|944->927|944->927|945->928|947->930|947->930|948->931|948->931|950->933|954->937|954->937|956->939|956->939|956->939|957->940|960->943|960->943|962->945|962->945|962->945|963->946|967->950|967->950|969->952|969->952|969->952|970->953|970->953|970->953|971->954|972->955|972->955|974->957|981->964|981->964|984->967|984->967|984->967|985->968|985->968|985->968|986->969|987->970|987->970|989->972|992->975|992->975|994->977|994->977|994->977|995->978|995->978|995->978|996->979|997->980|997->980|999->982|1003->986|1003->986|1005->988|1006->989|1006->989|1007->990|1010->993|1010->993|1012->995|1012->995|1012->995|1013->996|1015->998|1015->998|1017->1000|1017->1000|1017->1000|1018->1001|1020->1003|1020->1003|1022->1005|1022->1005|1022->1005|1023->1006|1023->1006|1023->1006|1024->1007|1025->1008|1025->1008|1026->1009|1026->1009|1026->1009|1027->1010|1028->1011|1028->1011|1029->1012|1030->1013|1030->1013|1031->1014|1115->1098|1115->1098|1115->1098|1116->1099|1116->1099|1116->1099|1116->1099|1116->1099|1116->1099|1116->1099|1116->1099|1116->1099|1116->1099|1117->1100|1119->1102|1121->1104|1121->1104
                  -- GENERATED --
              */
          
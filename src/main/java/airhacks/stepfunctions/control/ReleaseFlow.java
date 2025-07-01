package airhacks.stepfunctions.control;

import java.util.Map;

import airhacks.cloudwatch.control.LogGroups;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.events.targets.SfnStateMachine;
import software.amazon.awscdk.services.stepfunctions.Activity;
import software.amazon.awscdk.services.stepfunctions.Chain;
import software.amazon.awscdk.services.stepfunctions.DefinitionBody;
import software.amazon.awscdk.services.stepfunctions.IChainable;
import software.amazon.awscdk.services.stepfunctions.IStateMachine;
import software.amazon.awscdk.services.stepfunctions.LogLevel;
import software.amazon.awscdk.services.stepfunctions.LogOptions;
import software.amazon.awscdk.services.stepfunctions.Pass;
import software.amazon.awscdk.services.stepfunctions.QueryLanguage;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.tasks.CallAwsService;
import software.constructs.Construct;

public interface ReleaseFlow {

    static SfnStateMachine create(Construct scope) {
        var logGroup = LogGroups.stepMachineExecution(scope);
        var successfulBuilds = LogGroups.successfulBuilds(scope);
        var stateMachine = StateMachine.Builder.create(scope, "ReleaseFlowMachine")
                .stateMachineName("ReleaseFlow")
                .queryLanguage(QueryLanguage.JSONATA)
                .definitionBody(releaseFlow(scope))
                .definitionSubstitutions(Map.of("stage", "integration"))
                .logs(LogOptions.builder()
                        .destination(logGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .timeout(Duration.hours(24))
                .build();
        successfulBuilds.grantWrite(stateMachine);
        return SfnStateMachine.Builder.create(stateMachine)
                .build();
    }

    static DefinitionBody releaseFlow(Construct scope) {
        var first = Pass.Builder.create(scope, "ExtractProjectName")
                .assign(Map.of(
                        "projectName", "{% $states.input.detail.`project-name` %}",
                        "buildStartTime", "{% $states.input.detail.`build-start-time` %}"))
                .build();
        var second = CallAwsService.Builder.create(scope, "WriteLog")
                .service("logs")
                .action("putLogEvents")
                .parameters(Map.of(
                        "logGroupName", "/airhacks/successful-builds",
                        "logStreamName", "{% 'release-' & $st   ate.projectName %}",
                        "logEvents", Map.of(
                                "timestamp", "{% $millis() %}",
                                "message",
                                "{% 'Project: ' & $state.projectName & ', Build Start: ' & $state.buildStartTime %}")))
                .build();

        var chain = Chain
                .start(first)
                .next(second);
        return DefinitionBody.fromChainable(chain);
    }
}

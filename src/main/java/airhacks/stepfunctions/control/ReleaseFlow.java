package airhacks.stepfunctions.control;

import java.util.List;
import java.util.Map;

import airhacks.cloudwatch.control.LogGroups;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.events.targets.SfnStateMachine;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.stepfunctions.Chain;
import software.amazon.awscdk.services.stepfunctions.DefinitionBody;
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
                                .definitionBody(releaseFlow(scope, successfulBuilds))
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

        static DefinitionBody releaseFlow(Construct scope, LogGroup successfulBuilds) {
                var configureVariables = Pass.Builder.create(scope, "ConfigureVariables")
                                .assign(Map.of(
                                                "projectName", "{% $states.input.detail.`project-name` %}",
                                                "buildStartTime",
                                                "{% $states.input.detail.`additional-information`.`build-start-time` %}",
                                                "buildNumber",
                                                "{% $states.input.detail.`additional-information`.`build-number` %}",
                                                "logStreamName",
                                                "{% 'release-' & $states.input.detail.`project-name` & '-' & $string($millis()) %}"))
                                .build();
                var logMessage = "{% 'Project: ' & $projectName & ', Build Number: ' & $buildNumber & ', Build Start: ' & $buildStartTime %}";

                var createStream = CallAwsService.Builder.create(scope, "CreateLogStream")
                                .service("cloudwatchlogs")
                                .action("createLogStream")
                                .iamResources(List.of(successfulBuilds.getLogGroupArn()))
                                .parameters(Map.of(
                                                "LogGroupName", "/airhacks/successful-builds",
                                                "LogStreamName", "{% $logStreamName %}"))
                                .build();
                var writeLog = CallAwsService.Builder.create(scope, "WriteLog")
                                .service("cloudwatchlogs")
                                .action("putLogEvents")
                                .iamResources(List.of(successfulBuilds.getLogGroupArn()))
                                .parameters(Map.of(
                                                "LogGroupName", "/airhacks/successful-builds",
                                                "LogStreamName", "{% $logStreamName %}",
                                                "LogEvents", List.of(
                                                                Map.of(
                                                                                "Timestamp", "{% $millis() %}",
                                                                                "Message",
                                                                                logMessage))))
                                .build();

                var chain = Chain
                                .start(configureVariables)
                                .next(createStream)
                                .next(writeLog);
                return DefinitionBody.fromChainable(chain);
        }
}

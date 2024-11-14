package airhacks.codepipeline.boundary;

import java.util.List;

import airhacks.codebuild.control.MavenCodeBuild;
import airhacks.s3.control.ArtifactBucket;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.codebuild.IProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.IAction;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionType;
import software.amazon.awscdk.services.codepipeline.actions.CodeStarConnectionsSourceAction;
import software.constructs.Construct;

public class CodepipelineStack extends Stack {
    static Artifact SOURCE_OUTPUT = Artifact.artifact("source");

    public CodepipelineStack(Construct scope, String projectName, String codestarConnectionARN) {
        super(scope, projectName + "-codepipeline");
        var artifactBucket = ArtifactBucket.create(this);
        var buildProject = MavenCodeBuild.createPipelineProject(this, artifactBucket, projectName);
        var pipeline = Pipeline.Builder.create(this, projectName + "Pipeline")
                .crossAccountKeys(true)
                .artifactBucket(artifactBucket)
                .pipelineName(projectName)
                .build();
        var owner = "AdamBien";
        var repository = "aws-quarkus-lambda-cdk-plain";
        var branch = "main";
        pipeline.addStage(createStage("github-checkout",
                List.of(createGithubConnection(codestarConnectionARN, owner, repository, branch))));
        pipeline.addStage(createStage("build-and-deploy", List.of(createCodeBuildAction(projectName,buildProject))));
        CfnOutput.Builder.create(this, "PipelineOutput").value(pipeline.getPipelineArn()).build();
    }

    StageOptions createStage(String stageName, List<IAction> actions) {
        return StageOptions.builder()
                .stageName(stageName)
                .actions(actions)
                .build();
    }

    CodeStarConnectionsSourceAction createGithubConnection(String codestarConnectionARN, String owner,
            String repository, String branchName) {
        return CodeStarConnectionsSourceAction.Builder.create()
                .actionName("checkout-from-github")
                .branch(branchName)
                .repo(repository)
                .owner(owner)
                .output(SOURCE_OUTPUT)
                .connectionArn(codestarConnectionARN)
                .build();
    }

    CodeBuildAction createCodeBuildAction(String projectName,IProject project) {
        return CodeBuildAction.Builder.create()
                .project(project)
                .actionName("build")
                .input(SOURCE_OUTPUT)
                .type(CodeBuildActionType.BUILD)
                .build();
    }
}

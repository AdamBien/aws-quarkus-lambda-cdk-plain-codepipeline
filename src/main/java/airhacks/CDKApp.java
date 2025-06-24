package airhacks;

import airhacks.codepipeline.boundary.CodepipelineStack;
import airhacks.codepipeline.boundary.GithubConfiguration;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Tags;

public class CDKApp {

    public static void main(final String[] args) {

        var app = new App();
        var appName = "aws-quarkus-lambda-cdk-plain";
        Tags.of(app).add("project", "airhacks.live");
        Tags.of(app).add("environment", "workshops");
        Tags.of(app).add("purpose", "serverless pipeline for aws-quarkus-lambda-cdk-plain");
        Tags.of(app).add("application", appName);

        var optionalConfiguration = Configuration.load();
        
        if(optionalConfiguration.isEmpty()) {
            return;
        }
        var configuration = optionalConfiguration.get();
        
        var owner = "AdamBien";
        var repository = "aws-quarkus-lambda-cdk-plain";
        var branch = "main";
        var githubConfiguration = new GithubConfiguration(owner, repository, branch);
        new CodepipelineStack(app, githubConfiguration,appName, configuration.codeStarConnectionARN);
        app.synth();
    }
}
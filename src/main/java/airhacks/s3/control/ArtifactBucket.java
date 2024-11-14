package airhacks.s3.control;

import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketAccessControl;
import software.constructs.Construct;

public interface ArtifactBucket {

    static Bucket create(Construct scope){
        return Bucket.Builder.create(scope, "ArtifactBucket")
                    .accessControl(BucketAccessControl.PRIVATE)
                    .build();
    }

    
}

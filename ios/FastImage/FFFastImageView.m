#import "FFFastImageView.h"
#import <React/UIView+React.h>

@interface FFFastImageView()

@property (nonatomic, assign) BOOL hasSentOnLoadStart;
@property (nonatomic, assign) BOOL hasCompleted;
@property (nonatomic, assign) BOOL hasErrored;

@property (nonatomic, strong) NSDictionary* onLoadEvent;

@end

@implementation FFFastImageView

- (id) init {
    self = [super init];
    self.resizeMode = RCTResizeModeCover;
    self.clipsToBounds = YES;
    return self;
}

- (void)dealloc {
    [NSNotificationCenter.defaultCenter removeObserver:self];
}

- (void)setResizeMode:(RCTResizeMode)resizeMode
{
    if (_resizeMode != resizeMode) {
        _resizeMode = resizeMode;
        self.contentMode = (UIViewContentMode)resizeMode;
    }
}

- (void)setOnFastImageLoadEnd:(RCTDirectEventBlock)onFastImageLoadEnd {
    _onFastImageLoadEnd = onFastImageLoadEnd;
    if (self.hasCompleted) {
        _onFastImageLoadEnd(@{});
    }
}

- (void)setOnFastImageLoad:(RCTDirectEventBlock)onFastImageLoad {
    _onFastImageLoad = onFastImageLoad;
    if (self.hasCompleted) {
        _onFastImageLoad(self.onLoadEvent);
    }
}

- (void)setOnFastImageError:(RCTDirectEventBlock)onFastImageError {
    _onFastImageError = onFastImageError;
    if (self.hasErrored) {
        _onFastImageError(@{});
    }
}

- (void)setOnFastImageLoadStart:(RCTDirectEventBlock)onFastImageLoadStart {
    if (_source && !self.hasSentOnLoadStart) {
        _onFastImageLoadStart = onFastImageLoadStart;
        onFastImageLoadStart(@{});
        self.hasSentOnLoadStart = YES;
    } else {
        _onFastImageLoadStart = onFastImageLoadStart;
        self.hasSentOnLoadStart = NO;
    }
}

- (void)sendOnLoad:(UIImage *)image {
    self.onLoadEvent = @{
                         @"width":[NSNumber numberWithDouble:image.size.width],
                         @"height":[NSNumber numberWithDouble:image.size.height]
                         };
    if (self.onFastImageLoad) {
        self.onFastImageLoad(self.onLoadEvent);
    }
}

- (void)imageDidLoadObserver:(NSNotification *)notification {
    FFFastImageSource *source = notification.object;
    if (source != nil && source.url != nil) {
        [self sd_setImageWithURL:source.url];
    }
}

- (void)didSetProps:(NSArray<NSString *> *)changedProps {
    if ([changedProps containsObject:@"source"]) {
        
        // Attach a observer to refresh other FFFastImageView instance sharing the same source
        [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(imageDidLoadObserver:) name:_source.url.absoluteString object:nil];

        // Load base64 images.
        NSString* url = [_source.url absoluteString];
        if (url && [url hasPrefix:@"data:image"]) {
            if (self.onFastImageLoadStart) {
                self.onFastImageLoadStart(@{});
                self.hasSentOnLoadStart = YES;
            } {
                self.hasSentOnLoadStart = NO;
            }
            UIImage *image = [UIImage imageWithData:[NSData dataWithContentsOfURL:_source.url]];
            [self setImage:image];
            if (self.onFastImageProgress) {
                self.onFastImageProgress(@{
                                       @"loaded": @(1),
                                       @"total": @(1)
                                       });
            }
            self.hasCompleted = YES;
            [self sendOnLoad:image];
            
            if (self.onFastImageLoadEnd) {
                self.onFastImageLoadEnd(@{});
            }
            return;
        }
        
        // Set headers.
        [_source.headers enumerateKeysAndObjectsUsingBlock:^(NSString *key, NSString* header, BOOL *stop) {
            [[SDWebImageDownloader sharedDownloader] setValue:header forHTTPHeaderField:key];
        }];
        
        // Set priority.
        SDWebImageOptions options = SDWebImageRetryFailed;
        switch (_source.priority) {
            case FFFPriorityLow:
                options |= SDWebImageLowPriority;
                break;
            case FFFPriorityNormal:
                // Priority is normal by default.
                break;
            case FFFPriorityHigh:
                options |= SDWebImageHighPriority;
                break;
        }
        
        switch (_source.cacheControl) {
            case FFFCacheControlWeb:
                options |= SDWebImageRefreshCached;
                break;
            case FFFCacheControlCacheOnly:
                options |= SDWebImageCacheMemoryOnly;
                break;
            case FFFCacheControlImmutable:
                break;
        }
        
        if (self.onFastImageLoadStart) {
            self.onFastImageLoadStart(@{});
            self.hasSentOnLoadStart = YES;
        } {
            self.hasSentOnLoadStart = NO;
        }
        self.hasCompleted = NO;
        self.hasErrored = NO;
        
        [self downloadImage:_source options:options];
    }
}

- (void)downloadImage:(FFFastImageSource *) source options:(SDWebImageOptions) options {
    __weak typeof(self) weakSelf = self; // Always use a weak reference to self in blocks

    // Load the new source.
    // This will work for:
    //   - https://
    //   - file:///var/containers/Bundle/Application/50953EA3-CDA8-4367-A595-DE863A012336/ReactNativeFastImageExample.app/assets/src/images/fields.jpg
    //   - file:///var/containers/Bundle/Application/545685CB-777E-4B07-A956-2D25043BC6EE/ReactNativeFastImageExample.app/assets/src/images/plankton.gif
    //   - file:///Users/dylan/Library/Developer/CoreSimulator/Devices/61DC182B-3E72-4A18-8908-8A947A63A67F/data/Containers/Data/Application/AFC2A0D2-A1E5-48C1-8447-C42DA9E5299D/Documents/images/E1F1D5FC-88DB-492F-AD33-B35A045D626A.jpg"
    NSData *data = [NSData dataWithContentsOfURL:_placeholder.url];
    UIImage *placeholderImage = data ? [UIImage imageWithData:data] : nil;

    [self sd_setImageWithURL:_source.url
            placeholderImage:placeholderImage
                     options:options
                    progress:^(NSInteger receivedSize, NSInteger expectedSize, NSURL * _Nullable targetURL) {
                        if (weakSelf.onFastImageProgress) {
                            weakSelf.onFastImageProgress(@{
                                                           @"loaded": @(receivedSize),
                                                           @"total": @(expectedSize)
                                                           });
                        }
                    } completed:^(UIImage * _Nullable image,
                                  NSError * _Nullable error,
                                  SDImageCacheType cacheType,
                                  NSURL * _Nullable imageURL) {
                        if (error) {
                            weakSelf.hasErrored = YES;
                                if (weakSelf.onFastImageError) {
                                    weakSelf.onFastImageError(@{});
                                }
                                if (weakSelf.onFastImageLoadEnd) {
                                    weakSelf.onFastImageLoadEnd(@{});
                                }
                        } else {
                            weakSelf.hasCompleted = YES;
                            [weakSelf sendOnLoad:image];

                            // Alert other FFFastImageView component sharing the same URL
                            [NSNotificationCenter.defaultCenter postNotificationName:source.url.absoluteString object:source];

                            if (weakSelf.onFastImageLoadEnd) {
                                weakSelf.onFastImageLoadEnd(@{});
                            }
                        }
                    }];
}

@end


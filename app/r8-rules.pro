# R8 configuration
# Disable aggressive optimizations to reduce memory usage
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 3
-allowaccessmodification
-dontpreverify

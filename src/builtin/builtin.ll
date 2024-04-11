; ModuleID = 'builtin.c'
source_filename = "builtin.c"
target datalayout = "e-m:e-p:32:32-i64:64-n32-S128"
target triple = "riscv32-unknown-unknown-elf"

@.str = private unnamed_addr constant [3 x i8] c"%s\00", align 1
@.str.1 = private unnamed_addr constant [4 x i8] c"%s\0A\00", align 1
@.str.2 = private unnamed_addr constant [3 x i8] c"%d\00", align 1
@.str.3 = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1

; Function Attrs: nounwind
define dso_local void @print(i8* noundef %0) local_unnamed_addr #0 {
  %2 = tail call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([3 x i8], [3 x i8]* @.str, i32 0, i32 0), i8* noundef %0) #10
  ret void
}

declare dso_local i32 @printf(i8* noundef, ...) local_unnamed_addr #1

; Function Attrs: nounwind
define dso_local void @println(i8* noundef %0) local_unnamed_addr #0 {
  %2 = tail call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([4 x i8], [4 x i8]* @.str.1, i32 0, i32 0), i8* noundef %0) #10
  ret void
}

; Function Attrs: nounwind
define dso_local void @printInt(i32 noundef %0) local_unnamed_addr #0 {
  %2 = tail call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([3 x i8], [3 x i8]* @.str.2, i32 0, i32 0), i32 noundef %0) #10
  ret void
}

; Function Attrs: nounwind
define dso_local void @printlnInt(i32 noundef %0) local_unnamed_addr #0 {
  %2 = tail call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([4 x i8], [4 x i8]* @.str.3, i32 0, i32 0), i32 noundef %0) #10
  ret void
}

; Function Attrs: nofree nounwind
define dso_local i8* @getString() local_unnamed_addr #2 {
  %1 = tail call dereferenceable_or_null(256) i8* @malloc(i32 noundef 256) #11
  %2 = tail call i32 (i8*, ...) @scanf(i8* noundef getelementptr inbounds ([3 x i8], [3 x i8]* @.str, i32 0, i32 0), i8* noundef %1) #11
  ret i8* %1
}

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #3

; Function Attrs: inaccessiblememonly mustprogress nofree nounwind willreturn
declare dso_local noalias noundef i8* @malloc(i32 noundef) local_unnamed_addr #4

; Function Attrs: nofree nounwind
declare dso_local noundef i32 @scanf(i8* nocapture noundef readonly, ...) local_unnamed_addr #5

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #3

; Function Attrs: nofree nounwind
define dso_local i32 @getInt() local_unnamed_addr #2 {
  %1 = alloca i32, align 4
  %2 = bitcast i32* %1 to i8*
  call void @llvm.lifetime.start.p0i8(i64 4, i8* nonnull %2) #12
  %3 = call i32 (i8*, ...) @scanf(i8* noundef getelementptr inbounds ([3 x i8], [3 x i8]* @.str.2, i32 0, i32 0), i32* noundef nonnull %1) #11
  %4 = load i32, i32* %1, align 4, !tbaa !4
  call void @llvm.lifetime.end.p0i8(i64 4, i8* nonnull %2) #12
  ret i32 %4
}

; Function Attrs: nofree nounwind
define dso_local noalias i8* @toString(i32 noundef %0) local_unnamed_addr #2 {
  %2 = tail call dereferenceable_or_null(16) i8* @malloc(i32 noundef 16) #11
  %3 = tail call i32 (i8*, i8*, ...) @sprintf(i8* noundef nonnull dereferenceable(1) %2, i8* noundef nonnull dereferenceable(1) getelementptr inbounds ([3 x i8], [3 x i8]* @.str.2, i32 0, i32 0), i32 noundef %0) #11
  ret i8* %2
}

; Function Attrs: nofree nounwind
declare dso_local noundef i32 @sprintf(i8* noalias nocapture noundef writeonly, i8* nocapture noundef readonly, ...) local_unnamed_addr #5

; Function Attrs: mustprogress nofree nounwind readonly willreturn
define dso_local i32 @string.length(i8* nocapture noundef readonly %0) local_unnamed_addr #6 {
  %2 = tail call i32 @strlen(i8* noundef nonnull dereferenceable(1) %0) #11
  ret i32 %2
}

; Function Attrs: argmemonly mustprogress nofree nounwind readonly willreturn
declare dso_local i32 @strlen(i8* nocapture noundef) local_unnamed_addr #7

; Function Attrs: nounwind
define dso_local i8* @string.substring(i8* noundef %0, i32 noundef %1, i32 noundef %2) local_unnamed_addr #0 {
  %4 = sub nsw i32 %2, %1
  %5 = add nsw i32 %4, 1
  %6 = tail call i8* @malloc(i32 noundef %5) #11
  %7 = getelementptr inbounds i8, i8* %0, i32 %1
  %8 = tail call i8* @memcpy(i8* noundef %6, i8* noundef %7, i32 noundef %4) #10
  %9 = getelementptr inbounds i8, i8* %6, i32 %4
  store i8 0, i8* %9, align 1, !tbaa !8
  ret i8* %6
}

declare dso_local i8* @memcpy(i8* noundef, i8* noundef, i32 noundef) local_unnamed_addr #1

; Function Attrs: nofree nounwind
define dso_local i32 @string.parseInt(i8* nocapture noundef readonly %0) local_unnamed_addr #2 {
  %2 = alloca i32, align 4
  %3 = bitcast i32* %2 to i8*
  call void @llvm.lifetime.start.p0i8(i64 4, i8* nonnull %3) #12
  %4 = call i32 (i8*, i8*, ...) @sscanf(i8* noundef %0, i8* noundef getelementptr inbounds ([3 x i8], [3 x i8]* @.str.2, i32 0, i32 0), i32* noundef nonnull %2) #11
  %5 = load i32, i32* %2, align 4, !tbaa !4
  call void @llvm.lifetime.end.p0i8(i64 4, i8* nonnull %3) #12
  ret i32 %5
}

; Function Attrs: nofree nounwind
declare dso_local noundef i32 @sscanf(i8* nocapture noundef readonly, i8* nocapture noundef readonly, ...) local_unnamed_addr #5

; Function Attrs: mustprogress nofree norecurse nosync nounwind readonly willreturn
define dso_local i32 @string.ord(i8* nocapture noundef readonly %0, i32 noundef %1) local_unnamed_addr #8 {
  %3 = getelementptr inbounds i8, i8* %0, i32 %1
  %4 = load i8, i8* %3, align 1, !tbaa !8
  %5 = zext i8 %4 to i32
  ret i32 %5
}

; Function Attrs: nounwind
define dso_local i8* @string.add(i8* noundef %0, i8* noundef %1) local_unnamed_addr #0 {
  %3 = tail call i32 @strlen(i8* noundef nonnull dereferenceable(1) %0) #11
  %4 = tail call i32 @strlen(i8* noundef nonnull dereferenceable(1) %1) #11
  %5 = add nsw i32 %4, %3
  %6 = add nsw i32 %5, 1
  %7 = tail call i8* @malloc(i32 noundef %6) #11
  %8 = tail call i8* @memcpy(i8* noundef %7, i8* noundef %0, i32 noundef %3) #10
  %9 = getelementptr inbounds i8, i8* %7, i32 %3
  %10 = tail call i8* @memcpy(i8* noundef %9, i8* noundef %1, i32 noundef %4) #10
  %11 = getelementptr inbounds i8, i8* %7, i32 %5
  store i8 0, i8* %11, align 1, !tbaa !8
  ret i8* %7
}

; Function Attrs: mustprogress nofree nounwind readonly willreturn
define dso_local zeroext i1 @string.equal(i8* nocapture noundef readonly %0, i8* nocapture noundef readonly %1) local_unnamed_addr #6 {
  %3 = tail call i32 @strcmp(i8* noundef nonnull dereferenceable(1) %0, i8* noundef nonnull dereferenceable(1) %1) #11
  %4 = icmp eq i32 %3, 0
  ret i1 %4
}

; Function Attrs: argmemonly mustprogress nofree nounwind readonly willreturn
declare dso_local i32 @strcmp(i8* nocapture noundef, i8* nocapture noundef) local_unnamed_addr #7

; Function Attrs: mustprogress nofree nounwind readonly willreturn
define dso_local zeroext i1 @string.notEqual(i8* nocapture noundef readonly %0, i8* nocapture noundef readonly %1) local_unnamed_addr #6 {
  %3 = tail call i32 @strcmp(i8* noundef nonnull dereferenceable(1) %0, i8* noundef nonnull dereferenceable(1) %1) #11
  %4 = icmp ne i32 %3, 0
  ret i1 %4
}

; Function Attrs: mustprogress nofree nounwind readonly willreturn
define dso_local zeroext i1 @string.less(i8* nocapture noundef readonly %0, i8* nocapture noundef readonly %1) local_unnamed_addr #6 {
  %3 = tail call i32 @strcmp(i8* noundef nonnull dereferenceable(1) %0, i8* noundef nonnull dereferenceable(1) %1) #11
  %4 = icmp slt i32 %3, 0
  ret i1 %4
}

; Function Attrs: mustprogress nofree nounwind readonly willreturn
define dso_local zeroext i1 @string.lessOrEqual(i8* nocapture noundef readonly %0, i8* nocapture noundef readonly %1) local_unnamed_addr #6 {
  %3 = tail call i32 @strcmp(i8* noundef nonnull dereferenceable(1) %0, i8* noundef nonnull dereferenceable(1) %1) #11
  %4 = icmp slt i32 %3, 1
  ret i1 %4
}

; Function Attrs: mustprogress nofree nounwind readonly willreturn
define dso_local zeroext i1 @string.greater(i8* nocapture noundef readonly %0, i8* nocapture noundef readonly %1) local_unnamed_addr #6 {
  %3 = tail call i32 @strcmp(i8* noundef nonnull dereferenceable(1) %0, i8* noundef nonnull dereferenceable(1) %1) #11
  %4 = icmp sgt i32 %3, 0
  ret i1 %4
}

; Function Attrs: mustprogress nofree nounwind readonly willreturn
define dso_local zeroext i1 @string.greaterOrEqual(i8* nocapture noundef readonly %0, i8* nocapture noundef readonly %1) local_unnamed_addr #6 {
  %3 = tail call i32 @strcmp(i8* noundef nonnull dereferenceable(1) %0, i8* noundef nonnull dereferenceable(1) %1) #11
  %4 = icmp sgt i32 %3, -1
  ret i1 %4
}

; Function Attrs: mustprogress nofree norecurse nosync nounwind readonly willreturn
define dso_local i32 @array.size(i8* nocapture noundef readonly %0) local_unnamed_addr #8 {
  %2 = getelementptr inbounds i8, i8* %0, i32 -4
  %3 = bitcast i8* %2 to i32*
  %4 = load i32, i32* %3, align 4, !tbaa !4
  ret i32 %4
}

; Function Attrs: mustprogress nofree nounwind willreturn
define dso_local noalias i8* @.malloc(i32 noundef %0) local_unnamed_addr #9 {
  %2 = shl nsw i32 %0, 2
  %3 = tail call i8* @malloc(i32 noundef %2) #11
  ret i8* %3
}

attributes #0 = { nounwind "frame-pointer"="none" "min-legal-vector-width"="0" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #1 = { "frame-pointer"="none" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #2 = { nofree nounwind "frame-pointer"="none" "min-legal-vector-width"="0" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #3 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #4 = { inaccessiblememonly mustprogress nofree nounwind willreturn "frame-pointer"="none" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #5 = { nofree nounwind "frame-pointer"="none" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #6 = { mustprogress nofree nounwind readonly willreturn "frame-pointer"="none" "min-legal-vector-width"="0" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #7 = { argmemonly mustprogress nofree nounwind readonly willreturn "frame-pointer"="none" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #8 = { mustprogress nofree norecurse nosync nounwind readonly willreturn "frame-pointer"="none" "min-legal-vector-width"="0" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #9 = { mustprogress nofree nounwind willreturn "frame-pointer"="none" "min-legal-vector-width"="0" "no-builtin-memcpy" "no-builtin-printf" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-features"="+a,+c,+m" }
attributes #10 = { nobuiltin nounwind "no-builtin-memcpy" "no-builtin-printf" }
attributes #11 = { "no-builtin-memcpy" "no-builtin-printf" }
attributes #12 = { nounwind }

!llvm.module.flags = !{!0, !1, !2}
!llvm.ident = !{!3}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 1, !"target-abi", !"ilp32"}
!2 = !{i32 1, !"SmallDataLimit", i32 8}
!3 = !{!"Ubuntu clang version 14.0.0-1ubuntu1.1"}
!4 = !{!5, !5, i64 0}
!5 = !{!"int", !6, i64 0}
!6 = !{!"omnipotent char", !7, i64 0}
!7 = !{!"Simple C/C++ TBAA"}
!8 = !{!6, !6, i64 0}

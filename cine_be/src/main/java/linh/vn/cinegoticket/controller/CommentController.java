package linh.vn.cinegoticket.controller;


import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.request.CommentCreateRequest;
import linh.vn.cinegoticket.dto.request.CommentUpdateRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.CommentResponse;
import linh.vn.cinegoticket.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    //Get
    @GetMapping("/{comment_id}")
    @PreAuthorize("hasRole('USER')")
    public CommentResponse _getComment(Principal principal, @PathVariable(value = "comment_id") String comment_id) {
        return commentService.getComment(principal.getName(), comment_id);
    }

    @GetMapping("/movie/{movie_id}")
    @PreAuthorize("hasRole('USER')")
    public List<CommentResponse> _getAllCommentsFromMovie(@PathVariable(value = "movie_id") long movie_id) {
        return commentService.getAllCommentsFromMovieId(movie_id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public List<CommentResponse> _getAllCommentsFromUsername(Principal principal) {
        return commentService.getAllCommentsFromusername(principal.getName());
    }

    @GetMapping("/user/{user_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CommentResponse> _getAllCommentsFromUserId(@PathVariable(value = "user_id") String user_id) {
        return commentService.getAllCommentsFromUserId(user_id);
    }

    @GetMapping("/getall")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CommentResponse> _getAllComments() {
        return commentService.getAllComments();
    }

    //create
    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public CommentResponse addComment(@RequestBody @Valid CommentCreateRequest req) {
        return commentService.addComment(req);
    }

    //update
    @PutMapping("/{comment_id}/edit")
    @PreAuthorize("hasRole('USER')")
    public CommentResponse _editCommentByUsername(Principal principal, @PathVariable(value = "comment_id") String comment_id,
                                                  @RequestBody @Valid CommentUpdateRequest req) {
        return commentService.editComment(principal.getName(), comment_id, req);
    }

    //delete
    @DeleteMapping("/{comment_id}/delete")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse _deleteCommentByUser(Principal principal, @PathVariable(value = "comment_id") String comment_id) {
        return commentService.deleteCommentByUser(principal.getName(), comment_id);
    }

    @DeleteMapping("/{comment_id}/admin_delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse _deleteCommentById(@PathVariable(value = "comment_id") String comment_id) {
        return commentService.deleteCommentById(comment_id);
    }
}

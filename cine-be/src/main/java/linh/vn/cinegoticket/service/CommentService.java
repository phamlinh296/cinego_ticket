package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.CommentCreateRequest;
import linh.vn.cinegoticket.dto.request.CommentUpdateRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.CommentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CommentService {
    public CommentResponse getComment(String username, String comment_id);

    public List<CommentResponse> getAllComments();

    public List<CommentResponse> getAllCommentsFromMovieId(long movie_id);

    public List<CommentResponse> getAllCommentsFromUserId(String user_id);

    public List<CommentResponse> getAllCommentsFromusername(String username);

    public CommentResponse addComment(CommentCreateRequest req);


    public ApiResponse addLike(String username, long movie_id);

    public ApiResponse addDisLike(String username, long movie_id);

    public CommentResponse editComment(String username, String comment_id, CommentUpdateRequest req);

    public ApiResponse deleteCommentByUser(String username, String comment_id);

    public ApiResponse deleteCommentById(String comment_id);
}

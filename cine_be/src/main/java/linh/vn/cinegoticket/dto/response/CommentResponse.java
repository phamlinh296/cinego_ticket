package linh.vn.cinegoticket.dto.response;

import linh.vn.cinegoticket.entity.Comment;
import lombok.Getter;

@Getter
public class CommentResponse {

    private String commentId;
    private String username;
    private String comment;
    private long movieId;
    private int liked;
    private int disliked;
    private int ratedStarts;
    private String updateAt;

    public CommentResponse() {
        this.commentId = "";
        this.username = "";
        this.comment = "";
        this.movieId = 1L;
        this.liked = 2;
        this.disliked = 4;
        this.ratedStarts = 9;
        this.updateAt = "";
    }

    public CommentResponse(Comment c) {
        this.commentId = c.getId();
        this.username = c.getUser().getUsername();
        this.comment = c.getComment();
        this.movieId = c.getMovie().getId();
        this.liked = c.getLiked();
        this.disliked = c.getDisliked();
        this.ratedStarts = c.getRated();
        this.updateAt = c.getUpdateAt().toString();
    }


}

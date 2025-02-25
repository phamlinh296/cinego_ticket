package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.request.CommentCreateRequest;
import linh.vn.cinegoticket.dto.request.CommentUpdateRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.CommentResponse;
import linh.vn.cinegoticket.entity.Comment;
import linh.vn.cinegoticket.entity.Movie;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.repository.BookingRepository;
import linh.vn.cinegoticket.repository.CommentRepository;
import linh.vn.cinegoticket.repository.MovieRepository;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private InputValidationService inputValidationService;

    @Override
    public CommentResponse getComment(String username, String comment_id) {
        Comment comment = commentRepository.findById(comment_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        return new CommentResponse(comment);
    }

    @Override
    public List<CommentResponse> getAllComments() {
        List<Comment> comments = commentRepository.findAll();

        List<CommentResponse> commentResponses = new ArrayList<>();
        for (Comment c : comments) {
            commentResponses.add(new CommentResponse(c));
        }
        return commentResponses;
    }

    @Override
    public List<CommentResponse> getAllCommentsFromMovieId(long movie_id) {
        List<Comment> comments = commentRepository.findAllByMovieId(movie_id);
        if (comments.isEmpty())
            System.out.println("No comment is found for this movie");

        List<CommentResponse> commentResponses = new ArrayList<>();
        for (Comment c : comments) {
            commentResponses.add(new CommentResponse(c));
        }
        return commentResponses;
    }

    @Override
    public List<CommentResponse> getAllCommentsFromUserId(String user_id) {
        List<Comment> comments = commentRepository.findAllByUserId(user_id);
        if (comments.isEmpty())
            System.out.println("No comment is found for this movie");

        List<CommentResponse> commentResponses = new ArrayList<>();
        for (Comment c : comments) {
            commentResponses.add(new CommentResponse(c));
        }
        return commentResponses;
    }

    @Override
    public List<CommentResponse> getAllCommentsFromusername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        return this.getAllCommentsFromUserId(user.getId());
    }

    @Override
    public CommentResponse addComment(CommentCreateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        Movie movie = movieRepository.findById(req.getMovieId()).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        //đk trc khi tạo comment: phải mua vé rồi và chưa review
//        Optional<Booking> booking = bookingRepository.findByUserIdAndMovieIdAndStatus(user.getId(), movie.getId(), BookingStatus.BOOKED);
//        if (booking.isEmpty())
//            throw new RuntimeException("You must buy ticket for this movie before reviewing.");
//        if (commentRepository.existsByUserIdAndMovieId(user.getId(), movie.getId()))
//            throw new RuntimeException("You already have reviewed this movie");

        // Kiểm tra số sao hợp lệ
        int star = req.getRatedStars();
        if (star > 5 || star < 0)
            throw new RuntimeException("Rating number must be in range 0 and 5");

        //tạo và save cmt
        Comment comment = new Comment(movie, user, star, req.getComment());
        commentRepository.save(comment);
        // Cập nhật movie và trả về response
        movie.addComment(comment);
        movieRepository.save(movie);

        return new CommentResponse(comment);
    }

    @Override
    public ApiResponse addLike(String username, long movie_id) {
        return null;
    }

    @Override
    public ApiResponse addDisLike(String username, long movie_id) {
        return null;
    }

    @Override
    public CommentResponse editComment(String username, String comment_id, CommentUpdateRequest req) {
        Comment comment = commentRepository.findById(comment_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        //comment do phai la cua username này moi dc edit
        if (!comment.getUser().getUsername().equals(username))
            throw new RuntimeException("This comment is not belonged to you");

        String validComment = inputValidationService.sanitizeInput(req.getComment());

        //số sao nếu khác bđ thì update lại (số sao ms cx phải 0<x<5)
        if (req.getRatingStars() != comment.getRated()) {
            if (req.getRatingStars() > 5 || req.getRatingStars() < 0)
                throw new RuntimeException("Rating number must be in range 0 and 5");
            comment.setRated(req.getRatingStars());
        }
        commentRepository.save(comment);
        return new CommentResponse(comment);
    }

    //xóa cmt theo id (dùng cho user nên cần check cmt có phải của user đó k)
    @Override
    public ApiResponse deleteCommentByUser(String username, String comment_id) {
        Comment comment = commentRepository.findById(comment_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        //thuoc ve username
        if (comment.getUser().getUsername() != username) {
            throw new RuntimeException("This comment is not belonged to you");
        }
        commentRepository.deleteById(comment_id);
        return new ApiResponse<>("Successfully deleted");
    }

    //xóa cmt theo id, k cần check vì admin đc xóa tất
    @Override
    public ApiResponse deleteCommentById(String comment_id) {
        Comment comment = commentRepository.findById(comment_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        commentRepository.deleteById(comment_id);
        return new ApiResponse<>("Successfully deleted");
    }
}

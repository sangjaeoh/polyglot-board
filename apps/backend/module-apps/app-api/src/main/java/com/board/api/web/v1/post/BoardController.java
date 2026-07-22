package com.board.api.web.v1.post;

import com.board.api.facade.BoardFacade;
import com.board.board.info.PostInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 CRUD 엔드포인트. */
@RestController
@RequestMapping(path = "/api/v1/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {

    private final BoardFacade boardFacade;

    public BoardController(BoardFacade boardFacade) {
        this.boardFacade = boardFacade;
    }

    /** 활성 게시글을 최신순으로 페이지 조회한다. 범위 밖 page·size는 400이다(계약의 min·max와 정합). */
    @GetMapping
    public PageResponse<PostSummaryResponse> listPosts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<PostInfo> posts = boardFacade.getPosts(page, size);
        return PageResponse.of(posts, PostSummaryResponse::from);
    }

    /** 게시글 상세를 조회한다. */
    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable UUID id) {
        return PostResponse.from(boardFacade.getPost(id));
    }

    /** 게시글을 작성한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody PostCreateRequest request) {
        return PostResponse.from(boardFacade.create(request.title(), request.content(), request.author()));
    }

    /** 게시글을 수정한다. */
    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable UUID id, @Valid @RequestBody PostUpdateRequest request) {
        boardFacade.update(id, request.title(), request.content());
        return PostResponse.from(boardFacade.getPost(id));
    }

    /** 게시글을 소프트삭제한다. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable UUID id) {
        boardFacade.delete(id);
    }
}

package com.board.api.web.v1.post;

import com.board.api.facade.BoardFacade;
import com.board.board.info.PostInfo;
import com.board.common.web.pagination.PaginationRequest;
import com.board.common.web.pagination.PaginationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 CRUD 엔드포인트. 에러 응답(400·404·500)은 OpenApiConfig OperationCustomizer가 계약에 부착한다. */
@RestController
@RequestMapping(path = "/api/v1/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {

    private final BoardFacade boardFacade;

    public BoardController(BoardFacade boardFacade) {
        this.boardFacade = boardFacade;
    }

    /** 활성 게시글을 최신순으로 페이지 조회한다. 요청은 1-based이고 범위 밖 page·size는 400이다(계약의 min·max와 정합). */
    @GetMapping
    @Operation(summary = "활성 게시글을 최신순으로 페이지 조회한다")
    @ApiResponse(responseCode = "200", description = "게시글 페이지(1-based)")
    public PaginationResponse<PostSummaryResponse> listPosts(@Valid @ParameterObject PaginationRequest pagination) {
        Page<PostInfo> posts = boardFacade.getPosts(pagination.zeroBasedPage(), pagination.size());
        return PaginationResponse.from(posts.map(PostSummaryResponse::from));
    }

    /** 게시글 상세를 조회한다. */
    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세를 조회한다")
    @ApiResponse(responseCode = "200", description = "게시글 상세")
    public PostResponse getPost(@Parameter(description = "게시글 ID") @PathVariable UUID id) {
        return PostResponse.from(boardFacade.getPost(id));
    }

    /** 게시글을 작성한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "게시글을 작성한다")
    @ApiResponse(responseCode = "201", description = "작성된 게시글")
    public PostResponse createPost(@Valid @RequestBody PostCreateRequest request) {
        return PostResponse.from(boardFacade.create(request.title(), request.content(), request.author()));
    }

    /** 게시글을 수정한다. */
    @PutMapping("/{id}")
    @Operation(summary = "게시글을 수정한다")
    @ApiResponse(responseCode = "200", description = "수정된 게시글")
    public PostResponse updatePost(
            @Parameter(description = "게시글 ID") @PathVariable UUID id, @Valid @RequestBody PostUpdateRequest request) {
        boardFacade.update(id, request.title(), request.content());
        return PostResponse.from(boardFacade.getPost(id));
    }

    /** 게시글을 소프트삭제한다. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "게시글을 소프트삭제한다")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    public void deletePost(@Parameter(description = "게시글 ID") @PathVariable UUID id) {
        boardFacade.delete(id);
    }
}

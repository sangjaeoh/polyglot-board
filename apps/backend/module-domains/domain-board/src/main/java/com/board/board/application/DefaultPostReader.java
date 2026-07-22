package com.board.board.application;

import com.board.board.application.info.PostInfo;
import com.board.board.application.provided.PostReader;
import com.board.board.application.required.PostRepository;
import com.board.board.domain.Post;
import com.board.board.domain.exception.PostNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link PostReader} 구현. 게시글 조회를 담당하고 Info 변환까지 트랜잭션 안에서 끝낸다. */
@Service
class DefaultPostReader implements PostReader {

    private final PostRepository postRepository;

    DefaultPostReader(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PostInfo getPost(UUID id) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(PostNotFoundException::new);
        return PostInfo.from(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostInfo> getPosts(Pageable pageable) {
        return postRepository
                .findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(pageable)
                .map(PostInfo::from);
    }
}

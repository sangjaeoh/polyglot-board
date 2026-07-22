package com.board.board.application;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import com.board.board.application.required.PostRepository;
import com.board.board.domain.exception.PostNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link DefaultPostRemover}가 활성 게시글이 없을 때(미존재·이미 삭제) 예외를 전파하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class DefaultPostRemoverTest {

    @Mock
    private PostRepository postRepository;

    @Test
    void removeThrowsWhenActivePostAbsent() {
        UUID id = UUID.randomUUID();
        given(postRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());
        DefaultPostRemover postRemover = new DefaultPostRemover(postRepository);

        assertThatExceptionOfType(PostNotFoundException.class).isThrownBy(() -> postRemover.remove(id));
    }
}

package com.board.board.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import com.board.board.exception.PostNotFoundException;
import com.board.board.repository.PostRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link PostModifier}가 활성 게시글이 없을 때(미존재·이미 삭제) 예외를 전파하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class PostModifierTest {

    @Mock
    private PostRepository postRepository;

    @Test
    void editThrowsWhenActivePostAbsent() {
        UUID id = UUID.randomUUID();
        given(postRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());
        PostModifier postModifier = new PostModifier(postRepository);

        assertThatExceptionOfType(PostNotFoundException.class).isThrownBy(() -> postModifier.edit(id, "제목", "본문"));
    }
}

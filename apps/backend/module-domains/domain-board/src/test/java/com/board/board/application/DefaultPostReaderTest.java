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

/** {@link DefaultPostReader}가 활성 게시글이 없을 때(미존재·이미 삭제) 예외를 전파하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class DefaultPostReaderTest {

    @Mock
    private PostRepository postRepository;

    @Test
    void getPostThrowsWhenActivePostAbsent() {
        UUID id = UUID.randomUUID();
        given(postRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());
        DefaultPostReader postReader = new DefaultPostReader(postRepository);

        assertThatExceptionOfType(PostNotFoundException.class).isThrownBy(() -> postReader.getPost(id));
    }
}

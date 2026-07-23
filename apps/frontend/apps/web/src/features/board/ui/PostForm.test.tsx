import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { PostForm } from './PostForm';
import type { FormState } from '../model/actions';

describe('PostForm', () => {
  it('초기값과 라벨을 채워 렌더한다', () => {
    const action = vi.fn(async (): Promise<FormState> => ({}));

    render(
      <PostForm
        action={action}
        submitLabel="작성"
        initial={{ title: '제목', content: '본문', author: '글쓴이' }}
        showAuthor
      />,
    );

    expect(screen.getByLabelText('제목')).toHaveValue('제목');
    expect(screen.getByLabelText('작성자')).toHaveValue('글쓴이');
    expect(screen.getByLabelText('내용')).toHaveValue('본문');
    expect(screen.getByRole('button', { name: '작성' })).toBeInTheDocument();
  });
});

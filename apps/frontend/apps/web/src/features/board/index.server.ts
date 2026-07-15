// 서버 라인 public API. RSC·라우트가 소비한다. 클라 컴포넌트는 이 배럴을 import하지 않는다.
export { getPosts, getPost } from './api/queries';
export {
  createPostAction,
  updatePostAction,
  deletePostAction,
  type FormState,
} from './model/actions';
export { PostList } from './ui/PostList';
export { ApiError } from '@board/api-client';

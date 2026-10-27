import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { getCategories } from '../api/categoryApi';
import { createProduct, getSellerProduct, stopProduct, updateSellerProduct } from '../api/productApi';
import type { CategoryListItem } from '../types/category';
import type { SellerProduct, ProductWriteRequest } from '../types/product';
import StopProductDialog from '../components/StopProductDialog';

function SellerProductFormPage() {
    const { productId } = useParams();
    const navigate = useNavigate();
    const editing = productId !== undefined;
    const id = Number(productId);
    const [categories, setCategories] = useState<CategoryListItem[]>([]);
    const [product, setProduct] = useState<SellerProduct | null>(null);
    const [name, setName] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [price, setPrice] = useState('');
    const [stock, setStock] = useState('');
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [error, setError] = useState('');
    const [pending, setPending] = useState(false);
    const [confirmStop, setConfirmStop] = useState(false);
    const [stopError, setStopError] = useState('');
    const [reload, setReload] = useState(0);

    useEffect(() => {
        let active = true;
        setLoading(true);
        setLoadError('');
        const load = async () => {
            if (editing && (!Number.isSafeInteger(id) || id < 1)) throw new Error('invalid id');
            const [list, current] = await Promise.all([getCategories(), editing ? getSellerProduct(id) : Promise.resolve(null)]);
            if (!active) return;
            setCategories(list);
            setProduct(current);
            setName(current?.productName ?? '');
            setCategoryId(current ? String(current.categoryId) : '');
            setPrice(current ? String(current.price) : '');
            setStock(current ? String(current.stock) : '');
        };
        load().catch(() => {
            if (active) setLoadError('상품 또는 카테고리를 불러오지 못했습니다. 본인 상품인지 확인해주세요.');
        }).finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [editing, id, reload]);

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (pending) return;
        setError('');
        const parsedPrice = Number(price);
        const parsedStock = Number(stock);
        if (!name.trim() || name.trim().length > 255 || !categories.some(category => String(category.categoryId) === categoryId)
            || price.trim() === '' || stock.trim() === ''
            || !Number.isInteger(parsedPrice) || parsedPrice < 0 || parsedPrice > 2147483647
            || !Number.isInteger(parsedStock) || parsedStock < 0 || parsedStock > 2147483647) {
            setError('상품명과 카테고리를 입력하고, 가격·재고는 0 이상의 정수로 입력해주세요.');
            return;
        }
        const request: ProductWriteRequest = {
            productName: name.trim(), categoryId: Number(categoryId), price: parsedPrice, stock: parsedStock,
            productStatus: product?.deleted ? 'STOPPED' : parsedStock === 0 ? 'SOLD_OUT' : 'SELLING',
        };
        setPending(true);
        try {
            if (editing) await updateSellerProduct(id, request);
            else await createProduct(request);
            navigate('/seller/products', { replace: true });
        } catch {
            setError('상품 저장에 실패했습니다. 입력 내용과 권한을 확인하고 다시 시도해주세요.');
        } finally { setPending(false); }
    };

    const handleStop = async () => {
        if (!product || pending) return;
        setPending(true); setStopError('');
        try {
            await stopProduct(product.productId);
            navigate('/seller/products', { replace: true });
        } catch { setStopError('판매중지에 실패했습니다. 다시 시도해주세요.'); }
        finally { setPending(false); }
    };

    return <main>
        <h1>{editing ? '상품 수정' : '상품 등록'}</h1>
        <Link to="/seller/products">내 판매상품</Link>
        {loading ? <p>상품 정보를 불러오는 중입니다.</p> : loadError
            ? <p role="alert">{loadError} <button onClick={() => setReload(value => value + 1)}>다시 시도</button></p>
            : <>
                {product?.deleted && <p>판매중지된 상품입니다. 정보를 수정해도 판매가 재개되지 않습니다.</p>}
                {categories.length === 0 && <p>등록 가능한 카테고리가 없습니다.</p>}
                <form onSubmit={event => void handleSubmit(event)}>
                    <fieldset disabled={pending || confirmStop}>
                        <legend>상품 정보</legend>
                        <p><label htmlFor="product-name">상품명</label>{' '}<input id="product-name" required maxLength={255}
                            value={name} onChange={event => setName(event.target.value)} /></p>
                        <p><label htmlFor="product-category">카테고리</label>{' '}<select id="product-category" required
                            value={categoryId} onChange={event => setCategoryId(event.target.value)}>
                            <option value="">카테고리 선택</option>
                            {product && !categories.some(category => category.categoryId === product.categoryId)
                                && <option value={product.categoryId} disabled>사용할 수 없는 기존 카테고리</option>}
                            {categories.map(category => <option key={category.categoryId} value={category.categoryId}>{category.categoryName}</option>)}
                        </select></p>
                        <p><label htmlFor="product-price">가격</label>{' '}<input id="product-price" type="number" required min="0" max="2147483647" step="1"
                            value={price} onChange={event => setPrice(event.target.value)} />원</p>
                        <p><label htmlFor="product-stock">재고</label>{' '}<input id="product-stock" type="number" required min="0" max="2147483647" step="1"
                            value={stock} onChange={event => setStock(event.target.value)} />개</p>
                        {error && <p role="alert">{error}</p>}
                        <button type="submit" disabled={categories.length === 0}>{pending ? '저장 중...' : editing ? '수정 저장' : '상품 등록'}</button>
                        {editing && <button type="button" disabled={product?.deleted} onClick={() => {
                            setStopError(''); setConfirmStop(true);
                        }}>판매중지</button>}
                    </fieldset>
                </form>
                {confirmStop && <StopProductDialog productName={name} pending={pending} error={stopError}
                    onConfirm={() => void handleStop()} onCancel={() => setConfirmStop(false)} />}
            </>}
    </main>;
}

export default SellerProductFormPage;

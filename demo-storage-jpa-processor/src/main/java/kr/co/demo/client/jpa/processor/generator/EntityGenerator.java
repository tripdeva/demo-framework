package kr.co.demo.client.jpa.processor.generator;

import com.squareup.javapoet.*;
import kr.co.demo.core.storage.annotation.*;
import kr.co.demo.client.jpa.processor.util.NamingUtils;
import kr.co.demo.core.storage.enums.CascadeType;
import kr.co.demo.core.storage.enums.EnumType;
import kr.co.demo.core.storage.enums.FetchType;
import kr.co.demo.core.storage.enums.RelationType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.List;

/**
 * JPA Entity 클래스 생성기
 *
 * <p>도메인 객체의 Storage 어노테이션을 분석하여
 * JPA Entity 클래스를 자동 생성합니다.
 *
 * <h2>어노테이션 변환 규칙</h2>
 * <table border="1">
 *     <tr><th>Storage 어노테이션</th><th>JPA 어노테이션</th></tr>
 *     <tr><td>{@code @StorageTable}</td><td>{@code @Entity}, {@code @Table}</td></tr>
 *     <tr><td>{@code @StorageId}</td><td>{@code @Id}, {@code @GeneratedValue}</td></tr>
 *     <tr><td>{@code @StorageColumn}</td><td>{@code @Column}</td></tr>
 *     <tr><td>{@code @StorageEnum}</td><td>{@code @Enumerated}</td></tr>
 *     <tr><td>{@code @StorageRelation}</td><td>{@code @ManyToOne}, {@code @OneToMany} 등</td></tr>
 *     <tr><td>{@code @StorageTransient}</td><td>필드 생성 제외</td></tr>
 * </table>
 *
 * <h2>생성 예시</h2>
 * <pre>{@code
 * // 입력: Order.java
 * @StorageTable("orders")
 * public class Order {
 *     @StorageId
 *     private Long id;
 * }
 *
 * // 출력: OrderEntity.java
 * @Entity
 * @Table(name = "orders")
 * public class OrderEntity {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *
 *     // getter, setter...
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 */
public class EntityGenerator {

	/** 소스 파일 생성을 위한 Filer */
	private final Filer filer;

	/**
	 * EntityGenerator 생성자
	 *
	 * @param filer 소스 파일 생성을 위한 Filer
	 */
	public EntityGenerator(Filer filer) {
		this.filer = filer;
	}

	/**
	 * 도메인 클래스로부터 JPA Entity를 생성합니다.
	 *
	 * <p>생성되는 파일 위치: {@code {packageName}.entity.{ClassName}Entity.java}
	 *
	 * @param domainClass 도메인 클래스의 TypeElement
	 * @param packageName 도메인 클래스의 패키지명
	 * @throws IOException 파일 생성 실패 시
	 */
	public void generate(TypeElement domainClass, String packageName) throws IOException {
		String domainClassName = domainClass.getSimpleName().toString();
		String entityClassName = domainClassName + "Entity";

		// @StorageTable에서 테이블명 추출 (미지정 시 스네이크 케이스 변환)
		StorageTable storageTable = domainClass.getAnnotation(StorageTable.class);
		String tableName = storageTable.value().isEmpty()
				? NamingUtils.toSnakeCase(domainClassName)
				: storageTable.value();

		// @Table 어노테이션 빌드 (indexes 포함)
		AnnotationSpec.Builder tableAnnotation = AnnotationSpec.builder(
						ClassName.get("jakarta.persistence", "Table"))
				.addMember("name", "$S", tableName);

		// @StorageIndex → @Table(indexes = {...})
		StorageIndex[] indexes = domainClass.getAnnotationsByType(StorageIndex.class);
		if (indexes.length > 0) {
			for (StorageIndex idx : indexes) {
				String idxName = idx.name().isEmpty()
						? "idx_" + tableName + "_" + String.join("_", idx.columns())
						: idx.name();
				String columnList = String.join(", ", idx.columns());
				AnnotationSpec indexAnnotation = AnnotationSpec.builder(
								ClassName.get("jakarta.persistence", "Index"))
						.addMember("name", "$S", idxName)
						.addMember("columnList", "$S", columnList)
						.addMember("unique", "$L", idx.unique())
						.build();
				tableAnnotation.addMember("indexes", "$L", indexAnnotation);
			}
		}

		// Auditing 어노테이션 필요 여부 확인
		boolean hasAuditing = domainClass.getEnclosedElements().stream()
				.anyMatch(e -> e.getAnnotation(StorageCreatedAt.class) != null
						|| e.getAnnotation(StorageUpdatedAt.class) != null);

		// Entity 클래스 빌더 생성
		TypeSpec.Builder entityBuilder = TypeSpec.classBuilder(entityClassName)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(ClassName.get("jakarta.persistence", "Entity"))
				.addAnnotation(tableAnnotation.build());

		if (hasAuditing) {
			entityBuilder.addAnnotation(AnnotationSpec.builder(
							ClassName.get("jakarta.persistence", "EntityListeners"))
					.addMember("value", "$T.class",
							ClassName.get("org.springframework.data.jpa.domain.support",
									"AuditingEntityListener"))
					.build());
		}

		// @StorageCompositeId 복합키 필드 수집
		List<VariableElement> compositeIdFields = new java.util.ArrayList<>();
		for (Element enclosed : domainClass.getEnclosedElements()) {
			if (enclosed.getKind() == ElementKind.FIELD
					&& enclosed.getAnnotation(StorageCompositeId.class) != null) {
				compositeIdFields.add((VariableElement) enclosed);
			}
		}

		// 복합키가 있으면 IdClass 생성 + @IdClass 어노테이션 추가
		if (!compositeIdFields.isEmpty()) {
			String idClassName = domainClassName + "EntityId";
			generateIdClass(compositeIdFields, packageName + ".entity", idClassName);
			entityBuilder.addAnnotation(AnnotationSpec.builder(
							ClassName.get("jakarta.persistence", "IdClass"))
					.addMember("value", "$T.class",
							ClassName.get(packageName + ".entity", idClassName))
					.build());
		}

		// 도메인 클래스의 필드 순회
		for (Element enclosed : domainClass.getEnclosedElements()) {
			if (enclosed.getKind() != ElementKind.FIELD) continue;

			VariableElement field = (VariableElement) enclosed;

			// @StorageTransient가 붙은 필드는 Entity에서 제외
			if (field.getAnnotation(StorageTransient.class) != null) continue;

			// 필드 생성
			FieldSpec fieldSpec = generateField(field, domainClassName);
			entityBuilder.addField(fieldSpec);

			// getter 메서드 생성
			entityBuilder.addMethod(generateGetter(field));

			// setter 메서드 생성
			entityBuilder.addMethod(generateSetter(field));
		}

		// JPA Entity는 기본 생성자 필수
		entityBuilder.addMethod(MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.build());

		// Java 파일 생성
		JavaFile javaFile = JavaFile.builder(packageName + ".entity", entityBuilder.build())
				.build();
		javaFile.writeTo(filer);
	}

	/**
	 * 도메인 필드를 JPA Entity 필드로 변환합니다.
	 *
	 * <p>필드에 붙은 Storage 어노테이션에 따라 적절한 JPA 어노테이션을 추가합니다.
	 *
	 * @param field           도메인 클래스의 필드
	 * @param domainClassName 도메인 클래스명 (연관관계 처리용)
	 * @return 생성된 FieldSpec
	 */
	private FieldSpec generateField(VariableElement field, String domainClassName) {
		String fieldName = field.getSimpleName().toString();
		TypeName fieldType = getFieldType(field, domainClassName);

		FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE);

		// @StorageId → @Id, @GeneratedValue
		StorageId storageId = field.getAnnotation(StorageId.class);
		if (storageId != null) {
			fieldBuilder.addAnnotation(ClassName.get("jakarta.persistence", "Id"));

			if (storageId.autoGenerated()) {
				fieldBuilder.addAnnotation(AnnotationSpec.builder(
								ClassName.get("jakarta.persistence", "GeneratedValue"))
						.addMember("strategy", "$T.IDENTITY",
								ClassName.get("jakarta.persistence", "GenerationType"))
						.build());
			}
			return fieldBuilder.build();
		}

		// @StorageCompositeId → @Id (복합키 구성 필드)
		StorageCompositeId compositeId = field.getAnnotation(StorageCompositeId.class);
		if (compositeId != null) {
			fieldBuilder.addAnnotation(ClassName.get("jakarta.persistence", "Id"));
			String colName = NamingUtils.toSnakeCase(fieldName);
			fieldBuilder.addAnnotation(AnnotationSpec.builder(
							ClassName.get("jakarta.persistence", "Column"))
					.addMember("name", "$S", colName)
					.build());
			return fieldBuilder.build();
		}

		// @StorageCreatedAt → @CreatedDate + @Column(updatable = false)
		StorageCreatedAt createdAt = field.getAnnotation(StorageCreatedAt.class);
		if (createdAt != null) {
			String colName = NamingUtils.toSnakeCase(fieldName);
			fieldBuilder.addAnnotation(ClassName.get(
					"org.springframework.data.annotation", "CreatedDate"));
			fieldBuilder.addAnnotation(AnnotationSpec.builder(
							ClassName.get("jakarta.persistence", "Column"))
					.addMember("name", "$S", colName)
					.addMember("updatable", "$L", false)
					.build());
			return fieldBuilder.build();
		}

		// @StorageUpdatedAt → @LastModifiedDate
		StorageUpdatedAt updatedAt = field.getAnnotation(StorageUpdatedAt.class);
		if (updatedAt != null) {
			String colName = NamingUtils.toSnakeCase(fieldName);
			fieldBuilder.addAnnotation(ClassName.get(
					"org.springframework.data.annotation", "LastModifiedDate"));
			fieldBuilder.addAnnotation(AnnotationSpec.builder(
							ClassName.get("jakarta.persistence", "Column"))
					.addMember("name", "$S", colName)
					.build());
			return fieldBuilder.build();
		}

		// @StorageVersion → @Version
		StorageVersion storageVersion = field.getAnnotation(StorageVersion.class);
		if (storageVersion != null) {
			fieldBuilder.addAnnotation(ClassName.get("jakarta.persistence", "Version"));
			return fieldBuilder.build();
		}

		// @StorageRelation → @ManyToOne, @OneToMany 등
		StorageRelation storageRelation = field.getAnnotation(StorageRelation.class);
		if (storageRelation != null) {
			addRelationAnnotation(fieldBuilder, storageRelation);
			return fieldBuilder.build();
		}

		// @StorageColumn → @Column
		StorageColumn storageColumn = field.getAnnotation(StorageColumn.class);
		String columnName = (storageColumn != null && !storageColumn.value().isEmpty())
				? storageColumn.value()
				: NamingUtils.toSnakeCase(fieldName);
		boolean nullable = storageColumn == null || storageColumn.nullable();
		boolean unique = storageColumn != null && storageColumn.unique();

		AnnotationSpec.Builder columnAnnotation = AnnotationSpec.builder(
						ClassName.get("jakarta.persistence", "Column"))
				.addMember("name", "$S", columnName);

		if (!nullable) {
			columnAnnotation.addMember("nullable", "$L", false);
		}
		if (unique) {
			columnAnnotation.addMember("unique", "$L", true);
		}

		fieldBuilder.addAnnotation(columnAnnotation.build());

		// @StorageEnum → @Enumerated
		StorageEnum storageEnum = field.getAnnotation(StorageEnum.class);
		if (storageEnum != null) {
			String enumType = storageEnum.value() == EnumType.STRING ? "STRING" : "ORDINAL";
			fieldBuilder.addAnnotation(AnnotationSpec.builder(
							ClassName.get("jakarta.persistence", "Enumerated"))
					.addMember("value", "$T.$L",
							ClassName.get("jakarta.persistence", "EnumType"), enumType)
					.build());
		}

		return fieldBuilder.build();
	}

	/**
	 * 필드의 타입을 결정합니다.
	 *
	 * <p>연관관계 필드의 경우 도메인 타입을 Entity 타입으로 변환합니다.
	 * <ul>
	 *     <li>{@code Order} → {@code OrderEntity}</li>
	 *     <li>{@code List<OrderItem>} → {@code List<OrderItemEntity>}</li>
	 * </ul>
	 *
	 * @param field           대상 필드
	 * @param domainClassName 도메인 클래스명
	 * @return 변환된 타입
	 */
	private TypeName getFieldType(VariableElement field, String domainClassName) {
		StorageRelation relation = field.getAnnotation(StorageRelation.class);

		// 일반 필드는 원본 타입 그대로 사용
		if (relation == null) {
			return TypeName.get(field.asType());
		}

		// 연관관계 필드: 타입을 Entity로 변환
		RelationType relationType = relation.type();

		// OneToMany, ManyToMany: List<Order> → List<OrderEntity>
		if (relationType == RelationType.ONE_TO_MANY || relationType == RelationType.MANY_TO_MANY) {
			TypeMirror typeMirror = field.asType();
			if (typeMirror instanceof DeclaredType) {
				DeclaredType declaredType = (DeclaredType) typeMirror;
				List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
				if (!typeArgs.isEmpty()) {
					String targetTypeName = typeArgs.get(0).toString();
					String simpleTargetName = targetTypeName.substring(targetTypeName.lastIndexOf('.') + 1);
					String targetPackage = targetTypeName.substring(0, targetTypeName.lastIndexOf('.'));
					ClassName entityType = ClassName.get(targetPackage + ".entity", simpleTargetName + "Entity");
					return ParameterizedTypeName.get(ClassName.get(List.class), entityType);
				}
			}
		} else {
			// ManyToOne, OneToOne: Order → OrderEntity
			String typeName = field.asType().toString();
			String simpleTypeName = typeName.substring(typeName.lastIndexOf('.') + 1);
			String typePackage = typeName.substring(0, typeName.lastIndexOf('.'));
			return ClassName.get(typePackage + ".entity", simpleTypeName + "Entity");
		}

		return TypeName.get(field.asType());
	}

	/**
	 * 연관관계 어노테이션을 추가합니다.
	 *
	 * <p>RelationType에 따라 적절한 JPA 어노테이션을 생성합니다.
	 * <ul>
	 *     <li>{@code MANY_TO_ONE} → {@code @ManyToOne}</li>
	 *     <li>{@code ONE_TO_MANY} → {@code @OneToMany}</li>
	 *     <li>{@code ONE_TO_ONE} → {@code @OneToOne}</li>
	 *     <li>{@code MANY_TO_MANY} → {@code @ManyToMany}</li>
	 * </ul>
	 *
	 * @param fieldBuilder 필드 빌더
	 * @param relation     StorageRelation 어노테이션
	 */
	private void addRelationAnnotation(FieldSpec.Builder fieldBuilder, StorageRelation relation) {
		RelationType type = relation.type();

		String jpaAnnotation = switch (type) {
			case MANY_TO_ONE -> "ManyToOne";
			case ONE_TO_MANY -> "OneToMany";
			case ONE_TO_ONE -> "OneToOne";
			case MANY_TO_MANY -> "ManyToMany";
		};

		AnnotationSpec.Builder builder = AnnotationSpec.builder(
				ClassName.get("jakarta.persistence", jpaAnnotation));

		// 양방향 관계의 mappedBy 처리
		if (!relation.mappedBy().isEmpty()) {
			builder.addMember("mappedBy", "$S", relation.mappedBy());
		}

		// cascade 처리
		CascadeType[] cascades = relation.cascade();
		if (cascades.length > 0) {
			ClassName jpaCascade = ClassName.get("jakarta.persistence", "CascadeType");
			if (cascades.length == 1) {
				builder.addMember("cascade", "$T.$L", jpaCascade, cascades[0].name());
			} else {
				StringBuilder cascadeCode = new StringBuilder("{");
				for (int i = 0; i < cascades.length; i++) {
					if (i > 0) cascadeCode.append(", ");
					cascadeCode.append("$T.").append(cascades[i].name());
				}
				cascadeCode.append("}");
				Object[] args = new Object[cascades.length];
				java.util.Arrays.fill(args, jpaCascade);
				builder.addMember("cascade", cascadeCode.toString(), args);
			}
		}

		// fetch 처리
		FetchType fetch = relation.fetch();
		if (fetch != FetchType.DEFAULT) {
			builder.addMember("fetch", "$T.$L",
					ClassName.get("jakarta.persistence", "FetchType"), fetch.name());
		}

		fieldBuilder.addAnnotation(builder.build());
	}

	/**
	 * 복합키용 IdClass를 생성합니다.
	 *
	 * @param fields      복합키 필드 목록
	 * @param packageName 생성 패키지
	 * @param className   IdClass 이름
	 * @throws IOException 파일 생성 실패 시
	 */
	private void generateIdClass(List<VariableElement> fields,
	                              String packageName, String className) throws IOException {
		TypeSpec.Builder idClassBuilder = TypeSpec.classBuilder(className)
				.addModifiers(Modifier.PUBLIC)
				.addSuperinterface(java.io.Serializable.class);

		// 필드 + getter + setter
		for (VariableElement field : fields) {
			String name = field.getSimpleName().toString();
			TypeName type = TypeName.get(field.asType());
			idClassBuilder.addField(FieldSpec.builder(type, name, Modifier.PRIVATE).build());
			idClassBuilder.addMethod(MethodSpec.methodBuilder("get" + NamingUtils.capitalize(name))
					.addModifiers(Modifier.PUBLIC).returns(type)
					.addStatement("return this.$N", name).build());
			idClassBuilder.addMethod(MethodSpec.methodBuilder("set" + NamingUtils.capitalize(name))
					.addModifiers(Modifier.PUBLIC).addParameter(type, name)
					.addStatement("this.$N = $N", name, name).build());
		}

		// 기본 생성자
		idClassBuilder.addMethod(MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC).build());

		// equals/hashCode (Objects 기반)
		ClassName objects = ClassName.get("java.util", "Objects");

		// equals
		MethodSpec.Builder equalsBuilder = MethodSpec.methodBuilder("equals")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(Object.class, "o")
				.returns(boolean.class)
				.addStatement("if (this == o) return true")
				.addStatement("if (o == null || getClass() != o.getClass()) return false")
				.addStatement("$L that = ($L) o", className, className);
		StringBuilder equalsExpr = new StringBuilder();
		for (int i = 0; i < fields.size(); i++) {
			String name = fields.get(i).getSimpleName().toString();
			if (i > 0) equalsExpr.append(" && ");
			equalsExpr.append("$T.equals(this.").append(name).append(", that.").append(name).append(")");
		}
		Object[] equalsArgs = new Object[fields.size()];
		java.util.Arrays.fill(equalsArgs, objects);
		equalsBuilder.addStatement("return " + equalsExpr, equalsArgs);
		idClassBuilder.addMethod(equalsBuilder.build());

		// hashCode
		StringBuilder hashArgs = new StringBuilder();
		for (int i = 0; i < fields.size(); i++) {
			if (i > 0) hashArgs.append(", ");
			hashArgs.append(fields.get(i).getSimpleName().toString());
		}
		idClassBuilder.addMethod(MethodSpec.methodBuilder("hashCode")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.returns(int.class)
				.addStatement("return $T.hash($L)", objects, hashArgs.toString())
				.build());

		JavaFile javaFile = JavaFile.builder(packageName, idClassBuilder.build()).build();
		javaFile.writeTo(filer);
	}

	/**
	 * getter 메서드를 생성합니다.
	 *
	 * @param field 대상 필드
	 * @return 생성된 getter MethodSpec
	 */
	private MethodSpec generateGetter(VariableElement field) {
		String fieldName = field.getSimpleName().toString();
		TypeName fieldType = TypeName.get(field.asType());

		// 연관관계 필드는 Entity 타입으로 변경
		StorageRelation relation = field.getAnnotation(StorageRelation.class);
		if (relation != null) {
			fieldType = getFieldType(field, "");
		}

		String methodName = "get" + NamingUtils.capitalize(fieldName);

		return MethodSpec.methodBuilder(methodName)
				.addModifiers(Modifier.PUBLIC)
				.returns(fieldType)
				.addStatement("return this.$N", fieldName)
				.build();
	}

	/**
	 * setter 메서드를 생성합니다.
	 *
	 * @param field 대상 필드
	 * @return 생성된 setter MethodSpec
	 */
	private MethodSpec generateSetter(VariableElement field) {
		String fieldName = field.getSimpleName().toString();
		TypeName fieldType = TypeName.get(field.asType());

		// 연관관계 필드는 Entity 타입으로 변경
		StorageRelation relation = field.getAnnotation(StorageRelation.class);
		if (relation != null) {
			fieldType = getFieldType(field, "");
		}

		String methodName = "set" + NamingUtils.capitalize(fieldName);

		return MethodSpec.methodBuilder(methodName)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(fieldType, fieldName)
				.addStatement("this.$N = $N", fieldName, fieldName)
				.build();
	}
}
